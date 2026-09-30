# Tasks: token-usage-metrics

**Input**: Design documents from `/specs/003-token-usage-metrics/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/chat-api.md](contracts/chat-api.md), [quickstart.md](quickstart.md)

**Scope**: Token usage visibility for the existing chat flow only. Do not implement sampling parameters, prompt templates, memory, persistence, billing, new endpoints, or features 004 and later.

## Phase 1: Setup and diagnosis

**Purpose**: Confirm the current response paths and provider API surface before changing code.

- [x] T001 Diagnose every current use of `content()` and every `ChatResponse` constructor in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`, `backend/src/main/java/com/example/chat/ChatController.java`, and `backend/src/main/java/com/example/chat/exception/GlobalExceptionHandler.java`; record confirmed findings and any Spring AI 2.0 accessor differences in `specs/003-token-usage-metrics/research.md`.
- [x] T002 Confirm the Spring AI 2.0.0 signatures for `call().chatResponse()`, usage metadata, model metadata, result output text, and finish reason using the dependency available to `backend/build.gradle` (depends on T001); document the verified mapping in `specs/003-token-usage-metrics/research.md`.
- [x] T003 [P] Record the current public request/response baseline and the fields that must remain compatible in `specs/003-token-usage-metrics/contracts/chat-api.md`.

## Phase 2: Foundational contract changes

**Purpose**: Establish the enriched internal result and stable public DTO shape before endpoint behavior is changed.

- [x] T004 Define the metadata-bearing internal result returned by `backend/src/main/java/com/example/chat/ModelService.java`, including nullable answer metadata for usage counts, model, finish reason, and provider total.
- [x] T005 Extend `backend/src/main/java/com/example/chat/dto/ChatResponse.java` with `promptTokens`, `completionTokens`, `totalTokens`, `model`, `finishReason`, and `tokensPerSecond`; preserve the empty Jackson constructor, existing getters/setters, and existing response fields.
- [x] T006 Define the null and arithmetic invariants in `backend/src/main/java/com/example/chat/dto/ChatResponse.java` and `specs/003-token-usage-metrics/data-model.md`: all metric fields serialize as `null` when unavailable, provider `totalTokens` is authoritative, and throughput is invalid when completion tokens or positive elapsed time are unavailable.

**Checkpoint**: The internal and public response contracts are explicit; implementation can proceed by user story.

## Phase 3: User Story 1 - Inspect chat token usage (Priority: P1) 🎯 MVP

**Goal**: Return real model usage metadata and throughput from the existing `POST /api/v1/chat` flow.

**Independent Test**: A deterministic MockMvc success test and a live Ollama request both return the four preserved fields plus the six requested metric fields, with provider values mapped without estimation.

### Tests for User Story 1

- [x] T007 [P] [US1] Add DTO serialization and empty-constructor tests for the preserved and added fields in `backend/src/test/java/com/example/chat/dto/ChatResponseTest.java`.
- [x] T008 [P] [US1] Add Spring AI metadata-mapping tests, including provider usage/model/finish reason, missing metadata, fully qualified provider response type handling, and two-decimal throughput expectations, in `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`.
- [x] T009 [P] [US1] Add a MockMvc success contract test asserting `requestId`, `answer`, `elapsedMs`, `status`, all six metric fields, provider total preservation, and the existing request shape in `backend/src/test/java/com/example/chat/ChatControllerTest.java`.

### Implementation for User Story 1

- [x] T010 [US1] Replace `content()` with `call().chatResponse()` in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`; map `response.getMetadata().getUsage()`, `response.getMetadata().getModel()`, `response.getResult().getOutput().getText()`, and result finish reason using the fully qualified `org.springframework.ai.chat.model.ChatResponse` name.
- [x] T011 [US1] Update `backend/src/main/java/com/example/chat/ChatController.java` to measure elapsed time with `System.nanoTime`, convert it to `elapsedMs`, calculate `tokensPerSecond` as `completionTokens / (elapsedMs / 1000)` rounded to two decimal places, and construct the enriched DTO without changing `POST /api/v1/chat`.
- [x] T012 [US1] Update `scripts/smoke_test.sh` to parse the response as JSON and assert the preserved fields plus all six metric field names without logging or echoing the submitted question or generated answer.
- [x] T013 [US1] Add a complete response-contract display in the static frontend served from `frontend/src/index.html`, preserving the existing chat request and keeping the endpoint response as the source of truth.

**Checkpoint**: US1 is the MVP and is independently demonstrable through MockMvc and the live Ollama smoke flow.

## Phase 4: User Story 2 - Preserve failure and compatibility behavior (Priority: P1)

**Goal**: Keep error paths, legacy fields, privacy requirements, and response construction complete.

**Independent Test**: MockMvc tests cover validation, model gateway failure, and runtime failure; each response keeps the applicable status/correlation behavior and includes unavailable metrics as `null`, while log assertions find no question, answer, tokenized text, or PII.

### Tests for User Story 2

- [x] T014 [P] [US2] Add MockMvc tests for validation errors, model gateway failures, runtime failures, complete metric-field nulls, and existing HTTP statuses in `backend/src/test/java/com/example/chat/ChatControllerTest.java`.
- [x] T015 [P] [US2] Add compatibility assertions for clients reading only `requestId`, `answer`, `elapsedMs`, and `status` in `backend/src/test/java/com/example/chat/ChatControllerTest.java`.
- [x] T016 [P] [US2] Add log-capture assertions proving questions, answers, tokenized text, and test PII are absent from controller/service/filter logs in `backend/src/test/java/com/example/chat/ChatControllerTest.java` and `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`.

### Implementation for User Story 2

- [x] T017 [US2] Update every error-response construction in `backend/src/main/java/com/example/chat/ChatController.java` and `backend/src/main/java/com/example/chat/exception/GlobalExceptionHandler.java` so the complete DTO shape is emitted with `null` metrics and existing status/correlation semantics.
- [x] T018 [US2] Remove question and serialized answer content from log statements in `backend/src/main/java/com/example/chat/ChatController.java` and `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`; retain only non-sensitive correlation, status, elapsed, model, and aggregate metrics.
- [x] T019 [US2] Review `backend/src/main/java/com/example/chat/filter/LogRedactionFilter.java` and adjust only the privacy behavior needed to ensure request logging remains content-free, without expanding this feature into a general logging redesign.

**Checkpoint**: US1 and US2 preserve the public contract, represent missing data consistently, and satisfy the privacy/test gates described by the constitution.

## Phase 5: Polish and cross-cutting validation

**Purpose**: Reconcile all design artifacts and enforce the explicit constitution gates.

- [x] T020 [P] Update `specs/003-token-usage-metrics/data-model.md` and `specs/003-token-usage-metrics/research.md` with any verified Spring AI accessor or nullability details discovered during implementation.
- [x] T021 Run `gradle -p backend test --no-daemon` and resolve failures in `backend/src/test/java/com/example/chat/` before marking the feature complete.
- [ ] T022 Run `docker compose -f docker/docker-compose.yml up -d --build` followed by `bash scripts/smoke_test.sh`; retain only non-sensitive validation output and confirm the response contract against `specs/003-token-usage-metrics/contracts/chat-api.md`.
- [ ] T023 Review backend logs from the deterministic and live flows for prohibited question, answer, tokenized-text, and PII content; do not mark the constitution gates passed if any prohibited content appears in `backend/src/main/java/com/example/chat/` logs.
- [x] T024 Confirm `specs/001-spring-ai-demo/` and `specs/002-migrate-to-ollama/` are unchanged and only the feature 003 scope was added, using `git status --short` and `specs/003-token-usage-metrics/spec.md`.
- [x] T025 Update `.github/workflows/smoke-test.yml` to run `gradle -p backend test --no-daemon` and the existing Docker smoke flow so CI covers deterministic backend tests and the complete chat path.

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: T001 and T002 run sequentially because both update `research.md`; T003 can run in parallel with them.
- **Phase 2 (Foundational)**: Depends on T001-T003; T004-T006 establish the shared response contract and block story work.
- **Phase 3 (US1 MVP)**: Depends on Phase 2; T007-T009 can be written first, then T010-T013.
- **Phase 4 (US2)**: Depends on T005-T006 and T010-T011; error and privacy tests must pass before the constitutional gates can pass.
- **Phase 5 (Polish)**: Depends on all desired stories; T021-T025 are final validation and constitutional gates.

### User Story Dependencies

- **US1 (P1)**: Starts after Phase 2 and is the MVP.
- **US2 (P1)**: Depends on the enriched DTO and service result from Phase 2/US1, but is independently testable through failure and compatibility requests.

## Parallel Execution Examples

### Setup

```text
T001 -> T002: Diagnose and confirm the provider mapping in sequence
T003: Record public contract baseline in parallel
```

### User Story 1

```text
T007: DTO serialization tests in backend/src/test/java/com/example/chat/dto/ChatResponseTest.java
T008: Service metadata tests in backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java
T009: MockMvc success contract in backend/src/test/java/com/example/chat/ChatControllerTest.java
```

### User Story 2

```text
T014: MockMvc error tests
T015: Compatibility assertions
T016: Log-capture assertions
```

## Implementation Strategy

### MVP First

1. Complete T001-T006.
2. Complete US1 T007-T013.
3. Run the US1 MockMvc and live smoke checks.
4. Stop for an MVP review only if the response contains real provider metrics and no new endpoint was introduced.

### Incremental Delivery

1. Add US2 error, compatibility, and privacy gates.
2. Run deterministic tests and inspect logs.
3. Run the final Gradle, Docker smoke, CI, log, and scope checks.

## Task Format Validation

All implementation tasks use `- [ ]`, sequential `T###` IDs, `[P]` only for independent work, `[US#]` on user-story tasks, and an explicit repository-relative file path. No task targets feature 004 or later.
