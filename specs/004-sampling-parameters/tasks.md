# Tasks: Per-Request Sampling Parameters

**Input**: Design documents from `/specs/004-sampling-parameters/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/chat-api.yaml`, `quickstart.md`

**Tests**: Automated tests are required by the specification and constitution. Within each user story, create the listed tests first and confirm they fail for the expected missing behavior before implementing the change.

**Organization**: Tasks are grouped by user story so each increment can be implemented and verified independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it changes a different file and has no dependency on an incomplete task in the same phase.
- **[Story]**: Maps the task to US1, US2, or US3 from `spec.md`.
- Every task names the exact file or files involved.

## Phase 1: Setup (Shared Review)

**Purpose**: Confirm the existing change surface and dependencies before editing code.

- [X] T001 Review the current request-to-model path in `backend/src/main/java/com/example/chat/dto/ChatRequest.java`, `backend/src/main/java/com/example/chat/ChatController.java`, `backend/src/main/java/com/example/chat/ModelService.java`, and `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java` before making changes
- [X] T002 [P] Verify existing backend dependencies in `backend/build.gradle` and identify Playwright in `frontend/package.json` as the only planned test dependency addition

**Checkpoint**: Existing contract, call chain, and available libraries are understood; no runtime configuration or container change is required.

---

## Phase 2: Foundational (Blocking Contract Checks)

**Purpose**: Protect the existing endpoint and validation entry point before user-story work.

**⚠️ CRITICAL**: Complete this phase before implementing any user story.

- [X] T003 Confirm that `@Valid` remains on the `ChatRequest` parameter, `BindingResult` is evaluated before the model call, and the endpoint remains `POST /api/v1/chat` in `backend/src/main/java/com/example/chat/ChatController.java`

**Checkpoint**: The validation entry point and versioned endpoint are fixed constraints for all following work.

---

## Phase 3: User Story 1 - Adjust sampling for one request (Priority: P1) 🎯 MVP

**Goal**: Accept any valid subset of the five fields, apply it only to the current Ollama call, preserve metrics, and isolate sequential and concurrent requests.

**Independent Test**: Submit all fields and partial subsets, then run sequential and concurrent requests with different values; each captured `OllamaChatOptions` must match only its request and every response must retain the feature 003 contract.

### Tests for User Story 1

- [X] T004 [P] [US1] Add failing service tests for all five valid values and at least two different partial subsets, capturing request-scoped `OllamaChatOptions` in `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`
- [X] T005 [P] [US1] Add failing MockMvc tests for valid full and partial sampling requests with the unchanged feature 003 response fields in `backend/src/test/java/com/example/chat/ChatControllerTest.java`

### Implementation for User Story 1

- [X] T006 [US1] Add nullable `Double temperature`, `Double topP`, `Integer topK`, `Integer numPredict`, and `Integer seed` fields with getters and setters in `backend/src/main/java/com/example/chat/dto/ChatRequest.java`
- [X] T007 [US1] Add inclusive Bean Validation annotations for `temperature` 0.0–2.0, `topP` 0.0–1.0, `topK` 0–200, and `numPredict` 1–2048 while leaving `seed` unrestricted in `backend/src/main/java/com/example/chat/dto/ChatRequest.java`
- [X] T008 [US1] Change the service boundary to receive the validated `ChatRequest` without altering `ModelResponse` in `backend/src/main/java/com/example/chat/ModelService.java` and update the invocation in `backend/src/main/java/com/example/chat/ChatController.java`
- [X] T009 [US1] Build a fresh `OllamaChatOptions.Builder` per call, set each supplied non-null field, and pass the built options through `.options(options)` before `.call().chatResponse()` in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`
- [X] T010 [US1] Update service stubs and regression assertions so success plus provider/runtime failure paths retain the feature 003 response shape, nullable metrics, and HTTP 502/500 behavior in `backend/src/test/java/com/example/chat/ChatControllerTest.java` and `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`
- [X] T011 [US1] Add sequential and concurrent service tests proving that different requests never share option values or mutate shared defaults in `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`
- [X] T012 [US1] Add an automated MockMvc acceptance test that sends 20 consecutive requests with different valid configurations without restarting the application and verifies all 20 are accepted in `backend/src/test/java/com/example/chat/ChatControllerTest.java`

**Checkpoint**: US1 is independently complete: full and partial configurations work, requests are isolated, 20 consecutive calls succeed, and feature 003 behavior remains stable.

---

## Phase 4: User Story 2 - Preserve configured defaults (Priority: P2)

**Goal**: Preserve global Ollama defaults for omitted/null fields and distinguish them from explicit valid zero values.

**Independent Test**: Compare omitted/null calls with explicit-zero calls; absent fields must not create runtime overrides, while valid zeros must be forwarded.

### Tests for User Story 2

- [X] T013 [P] [US2] Add failing service tests proving that omitted and explicit-null fields do not call `.options(...)` or populate an `OllamaChatOptions` override in `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`
- [X] T014 [P] [US2] Add failing MockMvc tests proving requests with omitted fields and fields explicitly set to null are accepted in `backend/src/test/java/com/example/chat/ChatControllerTest.java`
- [X] T015 [US2] Add failing service tests proving `temperature=0.0`, `topP=0.0`, `topK=0`, and `seed=0` are forwarded as explicit values in `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`

### Implementation for User Story 2

- [X] T016 [US2] Skip `.options(...)` entirely when all five fields are null and ensure a partially populated request leaves every absent builder property unset in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`

**Checkpoint**: Existing clients retain configured defaults and every permitted explicit zero remains intentional.

---

## Phase 5: User Story 3 - Reject unsafe or invalid values (Priority: P3)

**Goal**: Return a comprehensible HTTP 400 with safe field errors and prevent invalid requests from reaching the model.

**Independent Test**: Submit below-minimum and above-maximum values for each bounded field and verify HTTP 400, field details, null feature 003 metrics, no model call, and sanitized logs; verify minimum, maximum, and representative intermediate values are accepted.

### Tests for User Story 3

- [X] T017 [P] [US3] Create validation tests for each bounded field's minimum, maximum, representative intermediate value, immediately below/above-range values, one additional representative invalid value, mixed valid-invalid input, and `seed` values `Integer.MIN_VALUE`, `0`, and `Integer.MAX_VALUE` in `backend/src/test/java/com/example/chat/dto/ChatRequestTest.java`
- [X] T018 [P] [US3] Add failing MockMvc tests for HTTP 400, one error entry per invalid field, null feature 003 metrics, and zero model-service calls in `backend/src/test/java/com/example/chat/ChatControllerTest.java`

### Implementation for User Story 3

- [X] T019 [US3] Add an additive `errors` collection containing safe `field` and `message` values while preserving all existing constructors, fields, and metric nullability in `backend/src/main/java/com/example/chat/dto/ChatResponse.java`
- [X] T020 [US3] Map validation failures to HTTP 400 with a generated `requestId`, the stable general message in `answer`, `status=error`, `elapsedMs=0`, null metrics, and field errors in `backend/src/main/java/com/example/chat/ChatController.java`
- [X] T021 [US3] Replace full `BindingResult` logging with field names and validation codes only, then assert that question text, answers, and rejected values are absent from logs in `backend/src/main/java/com/example/chat/ChatController.java` and `backend/src/test/java/com/example/chat/ChatControllerTest.java`
- [X] T022 [US3] Verify specifically that `numPredict=1` and `numPredict=2048` are accepted while `0` and `2049` return HTTP 400 without invoking the model in `backend/src/test/java/com/example/chat/ChatControllerTest.java`

**Checkpoint**: Invalid input is rejected safely before model execution and the `numPredict` cap prevents unbounded requested output.

---

## Phase 6: Polish & Cross-Cutting Verification

**Purpose**: Verify the complete API and the constitution-required frontend-to-model path.

- [X] T023 [P] Extend API smoke checks with one valid sampling request and one invalid request returning HTTP 400 while retaining feature 003 assertions in `scripts/smoke_test.sh`
- [X] T024 Add an `@playwright/test` development dependency and reproducible `test:e2e` script in `frontend/package.json` and `frontend/package-lock.json`
- [X] T025 Create a browser acceptance test that opens the existing UI, submits a question, and verifies the rendered answer plus feature 003 metrics through the real backend/Ollama path in `tests/integration/frontend-chat-flow.spec.mjs`
- [X] T026 Add `npm ci`, `npx playwright install --with-deps chromium`, and the browser acceptance command after Docker startup and API smoke validation in `.github/workflows/smoke-test.yml`
- [X] T027 [P] Reconcile final field names, ranges, nullability, `answer` validation-message semantics, responses, and commands in `specs/004-sampling-parameters/contracts/chat-api.yaml`, `specs/004-sampling-parameters/data-model.md`, and `specs/004-sampling-parameters/quickstart.md`
- [X] T028 Run `gradle -p backend test --no-daemon` and resolve failures only in files listed by `specs/004-sampling-parameters/plan.md`
- [X] T029 Run the Docker, API smoke, and browser commands from `specs/004-sampling-parameters/quickstart.md`; verify the frontend flow and confirm `backend/src/main/resources/application.properties` was not edited for request-specific overrides

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 — Setup**: No dependencies; T001 and T002 can run in parallel.
- **Phase 2 — Foundational**: Depends on Phase 1 and fixes the endpoint/validation constraints.
- **Phase 3 — US1**: Depends on Phase 2 and delivers the independently testable MVP.
- **Phase 4 — US2**: Depends on US1 because it refines default-preservation behavior in the option builder.
- **Phase 5 — US3**: Depends on request fields from US1 and can proceed alongside US2 after T006–T009.
- **Phase 6 — Polish**: Depends on all three stories; T025 depends on T024 and T026 depends on T025.

### User Story Dependency Graph

```text
Setup → Foundation → US1 (P1 / complete MVP) → US2 (P2)
                                       └─────→ US3 (P3)
US2 + US3 → API smoke → browser regression → final verification
```

### Within Each User Story

- Add tests first and confirm failure for the expected missing behavior.
- Add or adjust DTO contracts before service mapping.
- Implement service behavior before completing endpoint assertions.
- Do not close a story until its independent test passes and feature 003 regression coverage remains green.

## Parallel Opportunities

- T001 and T002 can run in parallel.
- T004 and T005 can run in parallel because they modify different test files.
- T013 and T014 can run in parallel because they modify different test files.
- T017 and T018 can run in parallel because they modify different test files.
- After T006–T009, US2 and US3 can proceed concurrently while coordinating shared test files.
- T023 and T027 can run in parallel before final verification.

## Parallel Example: User Story 1

```text
Task T004: Service option-capture tests in SpringAiModelServiceTest.java
Task T005: HTTP success and metric-contract tests in ChatControllerTest.java
```

## Parallel Example: User Story 2

```text
Task T013: Service tests for omitted/null values in SpringAiModelServiceTest.java
Task T014: HTTP acceptance tests for omitted/null fields in ChatControllerTest.java
```

## Parallel Example: User Story 3

```text
Task T017: Bean Validation boundary tests in ChatRequestTest.java
Task T018: HTTP 400 and no-model-call tests in ChatControllerTest.java
```

## Implementation Strategy

### MVP First

1. Complete Setup and Foundational checks.
2. Complete US1 tasks T004–T012.
3. Verify full/partial inputs, sequential and concurrent isolation, 20 consecutive requests, and feature 003 regression behavior.
4. Continue with US2 and US3 only after US1 passes independently.

### Incremental Delivery

1. **US1**: Valid full/partial controls, isolation, and stable metrics.
2. **US2**: Null/omission default preservation and explicit-zero semantics.
3. **US3**: Boundaries, HTTP 400 details, sanitized logs, and `numPredict` cap.
4. **Polish**: API smoke, unchanged frontend browser regression, CI gate, and final local verification.

## Notes

- `[P]` means the task is safe to perform concurrently based on file ownership and dependencies.
- No task changes `application.properties` to apply request-specific sampling values.
- The visual client now exposes optional sampling controls so the implemented request contract can be exercised without external API tools; blank fields preserve defaults.
- No task adds a second endpoint, persistence, or runtime infrastructure outside feature 004.
- Keep commits small and tied to one task or a closely related task group.
