# Feature Specification: Per-Request Sampling Parameters

**Feature Branch**: `004-sampling-parameters`

**Created**: 2026-09-30

**Status**: Draft

**Input**: User description: "Permitir que el cliente controle los parametros de muestreo de Ollama en cada peticion al endpoint de chat, con valores opcionales, validacion de rangos, proteccion frente al consumo abusivo y conservacion de las metricas de la feature 003."

## Clarifications

### Session 2026-09-30

- Q: What response format must be used for sampling validation errors? → A: Preserve the existing error contract with `requestId`, `status`, and a general message, and add a collection of errors identified by field.
- Q: Which existing field carries the general validation message? → A: Use `answer` for the stable general validation message; do not introduce a separate `message` field.
- Q: What per-request limit applies to `numPredict`? → A: Enforce the inclusive range 1 through 2048 and reject values outside it before model execution.
- Q: What values and omission behavior apply to `seed`? → A: Accept any integer, including zero and negative values; preserve the model default only when `seed` is omitted or null.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Adjust sampling for one request (Priority: P1)

As an API client, I want to provide sampling parameters with an individual chat request so that I can control that response without editing global configuration, rebuilding containers, or restarting the application.

**Why this priority**: Per-request control is the core capability required by this feature.

**Independent Test**: Send otherwise identical chat requests with different valid sampling values while the application remains running, and verify that each request is accepted and returns the existing response contract and metrics.

**Acceptance Scenarios**:

1. **Given** the chat service is running, **When** a client sends a request with one or more valid sampling values, **Then** those values affect only that request and the response retains the metrics introduced by feature 003.
2. **Given** two sequential requests use different sampling values, **When** both are processed without restarting the application, **Then** each request uses its own values without changing the other request or the global defaults.
3. **Given** a client supplies only a subset of sampling values, **When** the request is processed, **Then** supplied values are used and omitted values retain their configured defaults.
4. **Given** concurrent requests use different sampling values, **When** they are processed, **Then** each request retains only its own values and neither mutates shared defaults.

---

### User Story 2 - Preserve configured defaults (Priority: P2)

As an existing chat client, I want requests that omit sampling controls to continue using the configured defaults so that the feature remains backward compatible.

**Why this priority**: Existing clients and baseline measurements must continue to behave as before the feature is introduced.

**Independent Test**: Send a request without sampling fields and another request with all sampling fields explicitly set to null, then verify that both are accepted without replacing configured defaults with zero values.

**Acceptance Scenarios**:

1. **Given** a request contains no sampling fields, **When** it is processed, **Then** all configured sampling defaults remain in effect.
2. **Given** optional sampling fields are explicitly null, **When** the request is processed, **Then** the corresponding configured defaults remain in effect.
3. **Given** a valid explicit value is zero, **When** zero is permitted for that parameter, **Then** it is treated as an intentional value rather than as an omitted field.

---

### User Story 3 - Reject unsafe or invalid values (Priority: P3)

As the service owner, I want invalid or excessive sampling values rejected before model execution so that clients receive clear feedback and cannot use the endpoint to request unbounded work.

**Why this priority**: Input validation protects contract correctness, service availability, and local compute resources.

**Independent Test**: Submit values below and above every permitted boundary and verify that each invalid request receives a client error and is not forwarded to the model.

**Acceptance Scenarios**:

1. **Given** `temperature` is outside 0.0 through 2.0, **When** the request is submitted, **Then** it is rejected with a comprehensible client error before model execution.
2. **Given** `topP` is outside 0.0 through 1.0 or `topK` is outside 0 through 200, **When** the request is submitted, **Then** it is rejected before model execution.
3. **Given** `numPredict` is outside 1 through 2048, **When** the request is submitted, **Then** it is rejected before model execution and no unbounded generation begins.
4. **Given** a request contains both valid and invalid fields, **When** it is submitted, **Then** the entire request is rejected and none of its values reach the model.

### Edge Cases

- An explicit `temperature` of 0.0 is valid and must not be interpreted as an omitted value.
- An explicit `topP` of 0.0 is valid and must not be interpreted as an omitted value.
- An explicit `topK` of 0 is valid and represents an intentional request value.
- Values exactly on each minimum or maximum boundary are accepted.
- A request may provide any subset of the five optional controls.
- A seed value of 0 or a negative integer is preserved as an explicit seed rather than treated as absent.
- Concurrent requests with different sampling values do not leak settings into one another.
- If the model fails after a valid request is accepted, the existing error contract remains stable and does not fabricate unavailable metrics.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow a chat request to optionally include `temperature`, `topP`, `topK`, `numPredict`, and `seed`.
- **FR-002**: The system MUST distinguish an omitted or null sampling value from an explicit numeric value, including valid zero values.
- **FR-003**: The system MUST preserve the configured default for every optional sampling value that is omitted or null.
- **FR-004**: The system MUST accept `temperature` values from 0.0 through 2.0, inclusive.
- **FR-005**: The system MUST accept `topP` values from 0.0 through 1.0, inclusive.
- **FR-006**: The system MUST accept `topK` values from 0 through 200, inclusive.
- **FR-007**: The system MUST accept `numPredict` values from 1 through 2048, inclusive, and MUST reject higher values before model execution as the per-request resource-consumption control for this feature.
- **FR-008**: The system MUST accept any integer `seed`, including zero and negative values, and MUST preserve the configured model behavior only when `seed` is omitted or null.
- **FR-009**: The system MUST reject any request containing an out-of-range sampling value with an HTTP 400 response that preserves the existing `requestId` and `status`, uses `answer` for a stable general validation message, and adds a collection identifying each invalid field and its validation error.
- **FR-010**: The system MUST reject invalid requests before sending any part of the request to the model.
- **FR-011**: Sampling values supplied for one request MUST NOT modify global configuration or affect later or concurrent requests.
- **FR-012**: The system MUST preserve the successful and error response shapes established by feature 003; error responses MUST retain every metric field and use null where model metadata is unavailable.
- **FR-013**: Successful responses MUST continue returning `promptTokens`, `completionTokens`, `totalTokens`, `model`, `finishReason`, `elapsedMs`, and `tokensPerSecond` with the nullability rules established by feature 003.
- **FR-014**: The system MUST support valid combinations of the optional sampling values rather than requiring all controls to be present together.
- **FR-015**: The system MUST NOT persist raw sampling requests, user questions, or model answers solely for this feature, and operational logging MUST continue to avoid or sanitize personal data.

### Key Entities

- **Chat Request**: An existing request containing a session identifier and question, optionally extended with per-request sampling controls.
- **Sampling Configuration**: The subset of optional controls explicitly supplied for one request; it exists only for that request and does not replace global defaults.
- **Chat Response**: The existing feature 003 response containing model output, status, elapsed time, token metrics, model identity, finish reason, and generation speed.
- **Validation Error**: A client-facing response that preserves the existing error contract and includes a collection identifying one or more invalid fields without invoking the model.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An automated acceptance test submits 20 consecutive requests with different valid sampling configurations in one running environment without editing configuration, rebuilding, or restarting services, and all 20 are accepted for processing.
- **SC-002**: Automated validation tests accept the documented minimum, maximum, and at least one representative intermediate value for each bounded sampling parameter, subject only to independent model availability in live checks.
- **SC-003**: Automated validation tests reject the value immediately below and immediately above every documented bounded range, plus at least one additional representative invalid value per bounded parameter, before model execution and with field-level validation details.
- **SC-004**: Requests with omitted or null sampling fields preserve configured defaults in all acceptance tests, while explicit valid zero values remain distinguishable from omission.
- **SC-005**: Every successful chat response retains the feature 003 fields `promptTokens`, `completionTokens`, `totalTokens`, `model`, `finishReason`, `elapsedMs`, and `tokensPerSecond` with their established nullability rules.
- **SC-006**: Automated coverage verifies the core path for valid, omitted, boundary, invalid, and mixed sampling inputs, including at least one integration-level flow.

## Assumptions

- Existing global model defaults remain the baseline whenever a request omits an optional sampling control.
- `topK` equal to 0 is an intentional valid value used to disable that sampling restriction for the current request.
- Seed accepts the full integer domain supported by the existing request contract; explicit zero and negative values are forwarded, while omission or null preserves the model default.
- The output limit of 2048 is the selected safety boundary for this demo and prevents clients from requesting unbounded generation.
- Feature 003 is complete and its response fields and nullability decisions remain authoritative.
- New visual controls for sampling parameters are outside the minimum viable scope; however, the existing frontend-to-backend-to-model flow remains covered by an automated browser regression test as required by the project constitution.
