# Feature Specification: token-usage-metrics

**Feature Branch**: `003-token-usage-metrics`

**Created**: 2026-09-30

**Status**: Draft

**Input**: User request to expose token consumption for the existing chat endpoint while preserving its current response fields and using the real token metadata returned by the configured local model.

## Clarifications

### Session 2026-09-30

- Q: How should a metric be represented when Spring AI or Ollama does not provide it? -> A: Always include the metric field and use `null` when the value is unavailable.
- Q: How should a provider-reported `totalTokens` value that differs from the component sum be handled? -> A: Preserve the provider's `totalTokens` and document the discrepancy.
- Q: Which elapsed interval should be used for `tokensPerSecond`? -> A: Use the existing endpoint `elapsedMs` value.
- Q: How should an error response without model metadata represent the response contract? -> A: Include all metric fields with `null` values.
- Q: What test strategy must validate the metrics and contract? -> A: Deterministic unit tests plus an end-to-end test against real Ollama.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Inspect chat token usage (Priority: P1)

As a developer or evaluator, I can submit a question to the existing chat endpoint and inspect the model, completion status, token counts, elapsed time, and output-token throughput alongside the answer.

**Why this priority**: Token usage is the feature's primary value and must be available on the existing chat flow without requiring a new endpoint or changing how clients submit questions.

**Independent Test**: Run the existing end-to-end chat flow against a running local model, then verify that the successful response contains the preserved fields and all token-metric fields populated from the model response.

**Acceptance Scenarios**:

1. **Given** the chat service and local model are available, **When** a client sends a valid request to `POST /api/v1/chat`, **Then** the response retains `requestId`, `answer`, `elapsedMs`, and `status`, and also includes `promptTokens`, `completionTokens`, `totalTokens`, `model`, `finishReason`, and `tokensPerSecond`.
2. **Given** a successful response with token counts, **When** a client compares the counts, **Then** `totalTokens` is comparable to `promptTokens + completionTokens` and any provider-specific discrepancy is represented consistently and documented.
3. **Given** a successful response with a positive elapsed time, **When** a client reads `tokensPerSecond`, **Then** it represents output tokens divided by elapsed time using a consistent time unit.

---

### User Story 2 - Preserve failure and compatibility behavior (Priority: P1)

As a client of the existing chat API, I can continue using the same request endpoint and rely on a structured response when model processing fails, without losing correlation or receiving user content in logs.

**Why this priority**: Existing clients must not break while the response gains observability data, and error behavior is part of the endpoint contract.

**Independent Test**: Exercise invalid input and an unavailable or failing model, then verify the existing error status and correlation behavior remain usable and that all six metric fields are present with `null` when unavailable rather than fabricated.

**Acceptance Scenarios**:

1. **Given** a request that cannot be processed by the model, **When** `POST /api/v1/chat` returns an error, **Then** the response keeps the applicable `requestId`, `elapsedMs`, and `status` behavior and does not invent token counts, model metadata, or throughput values that were not supplied by the model.
2. **Given** an existing client that reads `requestId`, `answer`, `elapsedMs`, and `status`, **When** it calls the updated endpoint, **Then** those fields remain available with their existing meanings and the endpoint path and request shape remain unchanged.

---

### Edge Cases

- The model response may omit usage metadata or provide zero/unknown elapsed time; the API must not fabricate counts or divide by zero, and unavailable metrics must be represented consistently.
- A provider may report `totalTokens` that does not exactly equal the sum of prompt and completion tokens; the provider value remains authoritative and the response or documentation must make the discrepancy observable.
- The model may stop for different reasons or return no finish reason; the API must preserve an unavailable value without failing an otherwise valid chat response.
- A model failure may occur before any model response metadata exists; error handling must retain the complete response shape with metric fields set to `null` and must not log the question, answer, or personal information.
- Existing clients that ignore newly added fields must continue to parse the response successfully.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST continue accepting valid chat requests through the existing `POST /api/v1/chat` endpoint without changing its request shape.
- **FR-002**: A successful chat response MUST retain the fields `requestId`, `answer`, `elapsedMs`, and `status` with their existing meanings.
- **FR-003**: A successful chat response MUST add `promptTokens`, `completionTokens`, `totalTokens`, `model`, `finishReason`, and `tokensPerSecond`.
- **FR-004**: The token counts, model identifier, and finish reason MUST come from the actual response metadata produced by the configured Spring AI and Ollama model interaction; the system MUST NOT estimate or substitute values when provider metadata is available.
- **FR-005**: `totalTokens` MUST preserve the provider-reported value and remain directly comparable with `promptTokens + completionTokens`; any difference MUST be observable and documented rather than hidden or normalized.
- **FR-006**: `tokensPerSecond` MUST be calculated as `completionTokens / (elapsedMs / 1000)` using the existing endpoint `elapsedMs` value, rounded to two decimal places, and MUST be unavailable rather than invalid when the denominator or completion count cannot support a calculation.
- **FR-007**: Error responses MUST preserve the endpoint's existing status and correlation behavior, include all metric fields, and represent unavailable usage data as `null` rather than fabricated values.
- **FR-008**: The feature MUST update every response-construction path affected by the chat response contract, including success and applicable error paths.
- **FR-009**: Application logs MUST NOT record the submitted question, generated answer, tokenized text, or personal information; logs MAY include non-sensitive correlation and aggregate diagnostic metadata needed to troubleshoot the flow.
- **FR-010**: Automated tests MUST cover the complete request path from client submission through model response to the returned JSON, including preservation of existing fields, new metrics, error behavior, log redaction, and compatibility with clients that ignore added fields.
- **FR-011**: The feature MUST remain limited to token usage visibility; sampling parameters, prompt templates, conversation memory, and later-block functionality are out of scope.

### Key Entities

- **ChatResponse**: The existing response returned by the chat endpoint, preserving request correlation, answer, elapsed time, and status while carrying usage metadata.
- **TokenUsageMetrics**: Prompt-token count, completion-token count, total-token count, model identifier, finish reason, and output-token throughput associated with one model response.
- **ModelResponseMetadata**: Provider-produced usage and completion metadata from the actual model interaction.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of successful end-to-end chat test responses contain all four preserved response fields and all six requested token-usage fields.
- **SC-002**: In the automated successful-flow test, provider-reported counts are returned without estimation, and `totalTokens` can be compared directly with `promptTokens + completionTokens` for every response containing all three counts.
- **SC-003**: For successful responses with positive elapsed time and completion tokens, the reported throughput matches `completionTokens / (elapsedMs / 1000)` within `+/-0.01` tokens per second after rounding to two decimal places.
- **SC-004**: Automated error-flow tests cover model-unavailable or model-failure behavior and confirm no fabricated token metrics are returned.
- **SC-005**: Log inspection for the complete test flow finds zero occurrences of submitted questions, generated answers, tokenized text, or test personal information.
- **SC-006**: An existing client contract test that reads only `requestId`, `answer`, `elapsedMs`, and `status` passes unchanged against the updated response.

## Assumptions

- The existing backend already receives Spring AI and Ollama response metadata for the configured chat interaction or can expose that metadata without changing the public request contract.
- Every metric field remains present in the JSON response and uses `null` when the model does not provide the corresponding metadata.
- The existing endpoint `elapsedMs` field is the source for the throughput calculation; the result uses seconds as the denominator and is rounded to two decimal places with a `+/-0.01` tokens-per-second tolerance.
- Deterministic tests cover calculations, absent metadata, errors, and compatibility; the integration environment can run the local model needed for an end-to-end test against real Ollama.
- `specs/001-spring-ai-demo` and `specs/002-migrate-to-ollama` are outside this feature's scope and must not be modified.

## Out of Scope

- Sampling controls or other model-generation parameters.
- Prompt templates, conversation memory, persistence, or new chat workflows.
- New endpoints or changes to the existing chat request payload.
- Token billing, quotas, optimization recommendations, or provider comparisons.
