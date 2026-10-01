# Feature Specification: Conversation Memory

**Feature Branch**: `006-conversation-memory`

**Created**: 2026-10-01

**Status**: Draft

**Input**: User description: "Crear la feature 006-conversation-memory. El endpoint de chat debe recordar los mensajes de una misma conversación usando sessionId y mantener aisladas las conversaciones con identificadores distintos. Usar una ventana máxima de 20 mensajes que desaloje los antiguos y preserve los mensajes de sistema. Conservar las plantillas, parámetros por petición, métricas y contratos de error de las features 003–005. Incluir un script para la ejecución del experimento E de 20 turnos."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Continue a conversation by session (Priority: P1)

As a chat user, I want later messages with the same session identifier to include the relevant recent conversation so that I can ask follow-up questions without repeating prior context.

**Why this priority**: Remembering prior turns is the central user value of the feature and enables a coherent multi-turn chat.

**Independent Test**: Start a session, provide a synthetic fact, then ask a follow-up question using the same `sessionId`; verify that the answer can use the fact while the existing template selection, request parameters, response metrics, and error shape remain available.

**Acceptance Scenarios**:

1. **Given** a successful user/assistant exchange associated with a nonblank `sessionId`, **When** the next valid chat request uses the same identifier, **Then** the model receives the retained recent exchange before answering the new question.
2. **Given** several successful exchanges in one session, **When** the user asks a follow-up that depends on recent context, **Then** the response is generated with the retained messages in their original chronological order.
3. **Given** a request with no `sessionId` or an explicit null identifier, **When** it is processed, **Then** it remains stateless and preserves the behavior of existing clients.

---

### User Story 2 - Isolate conversations and bound their history (Priority: P2)

As a chat user, I want different session identifiers to have separate histories and old conversational messages to leave the active window so that one conversation neither leaks into another nor grows without bound.

**Why this priority**: Isolation protects user expectations and data boundaries, while a bounded window keeps the local demonstration predictable.

**Independent Test**: Interleave requests for two distinct session identifiers, place unique synthetic facts in each, and grow one session beyond 20 user/assistant messages; verify zero cross-session visibility, retention of the newest messages, eviction of the oldest messages, and continued inclusion of the applicable system instruction.

**Acceptance Scenarios**:

1. **Given** sessions A and B contain different synthetic facts, **When** each session asks about its own fact, **Then** each request receives only its own retained history.
2. **Given** interleaved or concurrent requests use different identifiers, **When** they complete, **Then** no message, template context, or request parameter from one identifier appears in the other identifier's retained conversation.
3. **Given** a session has more than 20 non-system messages, **When** its next request is prepared, **Then** only the newest messages up to that limit are retained in chronological order and the oldest non-system messages are evicted.
4. **Given** the non-system window reaches its limit, **When** old messages are evicted, **Then** the system instruction applicable to the current request is still present and does not consume one of the 20 positions.

---

### User Story 3 - Reproduce experiment E (Priority: P3)

As a workshop participant, I want one documented script that runs a 20-turn conversation so that I can reproduce and inspect the conversation-memory experiment locally.

**Why this priority**: A repeatable experiment makes the feature demonstrable without turning the experiment into an automatic production or CI workload.

**Independent Test**: Invoke the experiment script against the documented local environment and verify that it completes exactly 20 sequential user/assistant turns under one generated session identifier, checks the response contract on every turn, and produces a readable summary of continuity and window-eviction observations.

**Acceptance Scenarios**:

1. **Given** the documented local services are available, **When** a participant explicitly runs experiment E, **Then** the script sends exactly 20 sequential chat requests with one nonblank session identifier and reports each turn's outcome.
2. **Given** the 20 turns include fixed synthetic facts and later recall prompts, **When** the run finishes, **Then** the participant can inspect whether recent context was retained and context older than the 20-message window was displaced.
3. **Given** any turn returns a transport failure, an error response, or a success response that violates the established contract, **When** the script evaluates the result, **Then** it identifies the failing turn and exits unsuccessfully.
4. **Given** the repository's normal automated verification runs, **When** experiment E has not been explicitly requested, **Then** the 20-turn live-model experiment is not executed automatically.

### Edge Cases

- A `sessionId` is omitted, explicitly null, blank, or contains only whitespace.
- Two identifiers differ only by case or surrounding whitespace.
- Requests for different sessions arrive in an interleaved or concurrent order.
- Two requests for the same session overlap before either model response completes.
- A request is valid but the model fails before producing an answer.
- A request is rejected by input validation before model invocation.
- A successful response causes the session to cross exactly from 20 to more than 20 non-system messages.
- A request changes its registered template or controlled template context within an existing session.
- A session uses explicit zero sampling values after earlier turns used different values.
- A system instruction is present while the non-system history is already at its maximum size.
- The service restarts between two requests carrying the same identifier.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The chat operation MUST associate conversational history with each nonblank `sessionId` supplied by a client.
- **FR-002**: A valid chat request using a previously seen `sessionId` MUST include that session's retained conversational messages when generating the next answer.
- **FR-003**: Requests with distinct `sessionId` values MUST have isolated histories; a request MUST NOT receive messages stored under any other identifier.
- **FR-004**: Session identifiers MUST be matched as exact, case-sensitive opaque values and MUST NOT be trimmed, normalized, or derived from user content.
- **FR-005**: An omitted or explicit null `sessionId` MUST remain valid and MUST produce a stateless chat request. A blank or whitespace-only identifier MUST be rejected under the established validation error contract before model invocation.
- **FR-006**: Each session MUST retain no more than 20 non-system messages, counting user and successful assistant messages individually.
- **FR-007**: When adding messages would exceed the 20-message limit, the system MUST evict the oldest non-system messages first and retain the remaining messages in chronological order.
- **FR-008**: The system instruction applicable to the current request MUST always be included, MUST NOT count toward the 20-message conversational limit, and MUST NOT be displaced by non-system message eviction.
- **FR-009**: Only a successfully completed user/assistant exchange MUST become durable conversation context for a later request. Rejected requests, failed model calls, error payloads, partial answers, and experiment diagnostics MUST NOT be retained as conversational messages.
- **FR-010**: Requests for the same session that overlap in time MUST be incorporated one at a time in the order the service accepts them; each later accepted request MUST observe the successfully completed turn before it, without losing, duplicating, or reordering retained messages.
- **FR-011**: Template selection and controlled context values MUST remain request-scoped. A later request MUST use its own selected registered template and values while conversation history contributes only prior user and assistant messages.
- **FR-012**: The chat operation MUST preserve the registered templates, allowlist behavior, defaults, controlled variables, and unknown-property rejection established by feature 005.
- **FR-013**: The chat operation MUST preserve all per-request sampling controls, validation ranges, omission semantics, explicit-zero behavior, and isolation established by feature 004.
- **FR-014**: Every successful chat response MUST preserve the request, status, latency, token, model, completion, and throughput fields and their nullability rules established by feature 003.
- **FR-015**: Every validation or model failure MUST preserve the established safe error contract, including a generated nonblank request identifier, status, stable general message, nullable metrics, and safe field-level details where applicable.
- **FR-016**: The classification operation introduced by feature 005 MUST preserve its existing request, structured result, validation, metric, and error behavior and MUST remain stateless; `sessionId` MUST NOT be added to its accepted contract by this feature.
- **FR-017**: Operational logs MUST NOT contain raw session identifiers, user messages, assistant messages, rendered system instructions, or retained conversation contents.
- **FR-018**: Conversation memory MUST be limited to the running local service. Restarting the service MUST clear all retained session histories, and this feature MUST NOT add long-term conversation persistence or a user-facing history-management operation.
- **FR-019**: The repository MUST include an explicitly invoked experiment E script that sends exactly 20 sequential chat turns under one generated nonblank `sessionId` using fixed, non-sensitive synthetic content.
- **FR-020**: The experiment E script MUST validate the established success contract on every turn, identify the number of any failed turn, return a non-successful exit status on transport or contract failure, and present enough per-turn information to inspect recent-context retention and oldest-message eviction.
- **FR-021**: Experiment E MUST NOT run as part of the default build, unit-test, smoke-test, or continuous-integration workflow; its invocation prerequisites and expected output MUST be documented for local use.
- **FR-022**: Automated coverage MUST verify same-session recall, exact identifier isolation, the 20-message boundary and eviction order, preservation of system instructions, stateless requests, invalid identifiers, same-session concurrency, failure non-retention, and regressions across the feature 003–005 contracts.

### Key Entities

- **Conversation Session**: An isolated, runtime-scoped conversation identified by one exact opaque `sessionId`; it owns an ordered bounded history and does not imply a user identity.
- **Conversation Message**: A retained user or successful assistant message with a role and content, ordered within exactly one conversation session.
- **System Instruction**: The server-controlled instruction rendered from the registered template and controlled values for the current request; it is protected from conversational eviction and excluded from the 20-message count.
- **Message Window**: The newest zero to 20 non-system messages retained for one session after evicting the oldest messages.
- **Experiment E Run**: One explicitly started sequence of exactly 20 user/assistant turns using fixed synthetic content and one generated session identifier, with contract checks and a readable execution summary.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In automated same-session scenarios, 100% of follow-up requests receive the eligible retained messages in chronological order, with no missing or duplicated successfully completed turn.
- **SC-002**: Across sequential, interleaved, and concurrent tests using at least two distinct identifiers, zero messages or request-scoped settings cross session boundaries.
- **SC-003**: Boundary tests at 19, 20, and 21 non-system messages show a maximum of exactly 20 retained non-system messages, oldest-first eviction, and continued presence of the applicable system instruction.
- **SC-004**: 100% of omitted and null session identifiers remain stateless, while 100% of blank or whitespace-only identifiers are rejected before model invocation using the existing error contract.
- **SC-005**: Regression tests confirm that all documented template behavior, per-request sampling cases, success metrics, structured classification behavior, and safe error fields from features 003–005 retain their prior contracts.
- **SC-006**: Automated failure tests show that zero rejected requests, failed calls, partial answers, or error payloads appear in a later conversation context.
- **SC-007**: One explicit invocation of experiment E completes exactly 20 sequential turns, validates all 20 response envelopes, and emits a summary that identifies the run's session, completed-turn count, contract failures, recent-context observations, and window-eviction observations without using sensitive input.
- **SC-008**: The standard build, test, smoke, and continuous-integration commands make zero live invocations of the 20-turn experiment unless a participant explicitly runs its documented command.

## Assumptions

- One turn means one user request followed by one successful assistant response; therefore, 20 retained non-system messages normally represent 10 complete turns.
- The current request's user message participates in the 20-message input window; after a successful answer, the resulting user and assistant messages are subject to the same oldest-first bound for future requests.
- `sessionId` remains optional for backward compatibility. Memory is active only for a supplied nonblank identifier; omission or null retains stateless behavior.
- Session identifiers are opaque correlation values, not authenticated identities, and clients are responsible for generating sufficiently distinct, non-sensitive values and not sharing them with unrelated users.
- Conversation memory is process-local and intentionally disappears on service restart; cross-restart recovery, distributed instances, expiry timers, explicit clearing, listing, export, and deletion operations are outside this feature.
- Only the system instruction rendered for the current request is required; historical system instructions are not accumulated as conversational messages.
- Changing a template, its controlled context, or sampling values does not mutate retained user/assistant messages and does not become a default for later requests.
- Classification remains outside conversation memory because its feature 005 contract intentionally does not accept `sessionId`.
- Experiment E uses deterministic synthetic prompts to demonstrate continuity and the bounded window, but qualitative correctness of model prose remains an observation rather than a deterministic CI assertion.
