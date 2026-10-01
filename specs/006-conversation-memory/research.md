# Research: Conversation Memory

## Decision: Manage Spring AI chat memory explicitly

Use `MessageWindowChatMemory` with its in-memory repository and `maxMessages(20)`, but do not install `MessageChatMemoryAdvisor`. A dedicated conversation-memory service reads and commits messages only for a nonblank `sessionId`; requests without an identifier bypass memory, and classification continues through its existing stateless path.

**Rationale**: Spring AI's advisor requires a conversation ID on every advised request and stores the current user message before provider execution. A failed provider call can therefore leave an incomplete turn, which conflicts with success-only retention and nullable stateless requests. Direct use retains the framework's message abstraction and bounded store while giving the application control over transaction timing.

**Alternatives considered**: A global advisor was rejected because it breaks omitted/null stateless behavior and would affect classification. A per-request advisor was rejected because it still writes before success. A fully custom message model was rejected because Spring AI already supplies compatible message and memory abstractions.

**Primary references**: [Spring AI chat memory reference](https://docs.spring.io/spring-ai/reference/api/chat-memory.html), [Spring AI 2.0.0 `MessageChatMemoryAdvisor`](https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/MessageChatMemoryAdvisor.java), [Spring AI 2.0.0 `MessageWindowChatMemory`](https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/chat/memory/MessageWindowChatMemory.java)

## Decision: Keep the current system instruction outside conversation storage

Render exactly one system instruction from the current request's registered template and controlled context, place it first in the provider prompt, and store only user and assistant messages. For a stateful call, select the newest prior non-system messages so that adding the current user message never produces more than 20 non-system prompt messages.

**Rationale**: This makes the requested system instruction immune to conversation eviction, excludes it from the numeric window, prevents historical template/context values from becoming shared session state, and preserves feature 005's request-scoped registry behavior. The memory's successful-pair invariant keeps durable history at 20 or fewer; prompt selection independently covers 19/20/21-message boundary cases and evicts strictly oldest-first.

**Alternatives considered**: Storing system messages in `MessageWindowChatMemory` was rejected because the library preserves them but includes them when calculating its total size. Accumulating historical system instructions was rejected because later template selection must apply only to its own request. Rewriting history into one system string was rejected because it loses message roles.

## Decision: Commit one completed pair only after a nonblank answer

Hold a session's coordination gate across snapshot, prompt construction, provider call, response validation, and commit. Add `[UserMessage, AssistantMessage]` together only when the provider supplies a nonblank assistant answer. Exceptions, empty responses, rejected requests, errors, and diagnostics leave the prior snapshot unchanged.

**Rationale**: The HTTP controller already treats a blank answer as an unsuccessful response. Aligning commit with the same success boundary prevents later prompts from seeing content that clients experienced as failed and preserves chronological pairs in durable memory.

**Alternatives considered**: Writing the user before the call was rejected because rollback would require replacing the full history and introduces failure races. Writing blank assistant responses was rejected because they are surfaced as errors. Storing error envelopes was rejected because they are transport diagnostics rather than conversation messages.

## Decision: Serialize a session and allow different sessions to proceed independently

Use one fair admission gate per exact identifier. Define acceptance order for overlapping same-session requests as the order in which they acquire that gate. Keep the gate through the provider call, then release it after commit or failure. Exact different keys use different gates; null/omitted requests use no gate.

**Rationale**: `MessageWindowChatMemory.add()` performs read-process-save, and its in-memory repository only makes individual map operations thread-safe. Without a wider critical section, concurrent calls can both read the same snapshot, call the model without the previous turn, and overwrite each other. Per-session gates satisfy deterministic ordering without a global model bottleneck.

**Alternatives considered**: No lock was rejected because it loses updates. A global lock was rejected because unrelated conversations need not block each other. Optimistic retry was rejected because it could repeat an expensive model call. The number of session keys and gates remains unbounded for this demo because TTL and lifecycle APIs are explicitly out of scope.

## Decision: Preserve exact identifier semantics and a stateless fallback

Keep `sessionId` nullable and optional. Reject a supplied blank or whitespace-only value before model work. Use every other supplied value exactly as received: no trimming, case folding, hashing for identity, or derivation from content. Identifiers are not logged.

**Rationale**: This maintains existing-client compatibility while making memory activation explicit. Exact matching makes `case`, `Case`, and ` case ` isolated conversations as specified.

**Alternatives considered**: Requiring an identifier would break existing clients. Generating a server-side identifier for omitted input would silently turn stateless calls into unrelated stateful sessions. Normalization would merge identifiers the contract defines as distinct.

## Decision: Retain feature 003–005 request and response behavior

Apply the current request's registered template, controlled values, and fresh sampling options after history is selected. Extract usage, model, finish reason, elapsed time, and throughput exactly as today; provider-reported prompt tokens naturally cover the actual history sent. Keep all response DTOs and error statuses unchanged, and keep classification outside memory.

**Rationale**: Conversation state is input context, not a new response mode. The existing controller timer continues measuring user-observed operation latency, including any wait for an earlier request in the same session; provider failure timing remains within the existing model-operation error path.

**Alternatives considered**: Returning session metadata was rejected as an unnecessary contract change. Persisting sampling or template selection in the session was rejected because features 004 and 005 define them as request-scoped. Adding memory to classification was rejected because its contract does not accept `sessionId`.

## Decision: Keep experiment E explicit, synthetic, and observational

Add `scripts/experiment_e.sh` with Bash, curl, and jq. It generates one non-sensitive session identifier, sends exactly 20 fixed prompts sequentially, validates HTTP 200 and the complete established response envelope on every turn, identifies failures by turn, and prints contract and recall/eviction observations to standard output. Semantic recall indicators are reported but do not determine exit status; transport, HTTP, and schema failures do.

**Rationale**: Local-model prose is nondeterministic, while transport and contract behavior are deterministic. Fixed synthetic early/recent facts make continuity and eviction inspectable without collecting user data. Standard build, smoke, and CI stay bounded and never invoke the experiment.

**Alternatives considered**: A CI experiment was rejected for latency and nondeterminism. Persisting output by default was rejected for data minimization. Treating a model phrase match as a hard pass/fail gate was rejected as flaky.

