# Tasks: Conversation Memory

**Input**: Design documents from `/specs/006-conversation-memory/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/chat-api.yaml`, `quickstart.md`

**Tests**: Required by FR-022 and the project constitution. Write each story's failing tests before its implementation tasks and keep experiment E outside automated CI.

**Organization**: Tasks are grouped by user story so continuity, bounded isolation, and the experiment runner can be implemented and verified incrementally.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it touches different files and has no dependency on an incomplete task.
- **[Story]**: Maps work to User Story 1, 2, or 3.
- Every task names the exact file or files it changes or verifies.

## Phase 1: Setup and Baseline Review

**Purpose**: Confirm the current `sessionId` flow and establish a passing feature 005 baseline before introducing mutable runtime state.

- [X] T001 Review the current `sessionId` path and reconcile any divergence with the recorded baseline in `backend/src/main/java/com/example/chat/dto/ChatRequest.java`, `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`, `frontend/src/app/chat/chat.component.ts`, `frontend/src/app/services/chat.service.ts`, and `specs/006-conversation-memory/research.md`
- [X] T002 Run the existing backend and frontend test baselines defined by `backend/build.gradle` and `frontend/package.json`, recording any pre-existing failure before feature work
- [X] T003 Verify that Spring AI 2.0.0 already exposes `ChatMemory`, `MessageWindowChatMemory`, and `ChatClientRequestSpec.messages(List<Message>)` through `backend/build.gradle`; do not add a persistence starter or `MessageChatMemoryAdvisor`

**Checkpoint**: Feature 005 behavior is green and the implementation surface matches the plan.

---

## Phase 2: Foundational Memory Contract

**Purpose**: Establish request validation and the injectable, process-local memory foundation that blocks every user story.

**⚠️ CRITICAL**: No story implementation begins until this phase passes.

- [X] T004 [P] Add failing validation cases for omitted, null, blank, whitespace-only, case-distinct, and surrounding-whitespace `sessionId` values in `backend/src/test/java/com/example/chat/dto/ChatRequestTest.java`
- [X] T005 Add nullable-but-nonblank `sessionId` validation without trimming or normalization in `backend/src/main/java/com/example/chat/dto/ChatRequest.java`
- [X] T006 [P] Add a failing bean test proving one process-local `ChatMemory` with a 20-message maximum and no persistent repository in `backend/src/test/java/com/example/chat/memory/ConversationMemoryConfigurationTest.java`
- [X] T007 Configure `MessageWindowChatMemory` with the in-memory repository and `maxMessages(20)` in `backend/src/main/java/com/example/chat/memory/ConversationMemoryConfiguration.java`
- [X] T008 Create the injectable `ConversationMemoryService` boundary, 20-message constants, and exact-key session state in `backend/src/main/java/com/example/chat/memory/ConversationMemoryService.java`
- [X] T009 Run the focused foundational tests and compilation from `backend/build.gradle`, fixing only failures in `ChatRequestTest.java`, `ConversationMemoryConfigurationTest.java`, `ChatRequest.java`, and the new `memory/` package

**Checkpoint**: Null/omitted IDs remain valid, blank IDs fail validation, and a runtime-only ChatMemory bean is available without an automatic advisor.

---

## Phase 3: User Story 1 - Continue a Conversation by Session (Priority: P1) 🎯 MVP

**Goal**: A successful follow-up using the same exact `sessionId` receives prior user/assistant messages, while omitted or null identifiers remain stateless and feature 003–005 behavior is preserved.

**Independent Test**: Submit a synthetic fact and a follow-up under one ID, then submit equivalent omitted/null requests; capture the provider prompts and verify ordered same-session history only for the identified conversation, one provider call per request, current template/sampling values, unchanged metrics, and no retained failed turn.

### Tests for User Story 1 — write first and verify failure

- [X] T010 [P] [US1] Add failing same-session chronological recall, null/omitted stateless, successful-pair commit, provider-exception rollback, and blank-answer non-retention tests in `backend/src/test/java/com/example/chat/memory/ConversationMemoryServiceTest.java`
- [X] T011 [P] [US1] Add failing two-turn prompt-capture tests proving retained user/assistant roles, current system template/context, fresh sampling options, one provider call, and unchanged provider metrics in `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`
- [X] T012 [P] [US1] Add failing MockMvc cases for omitted/null success and blank/whitespace `sessionId` HTTP 400 envelopes with zero model calls in `backend/src/test/java/com/example/chat/ChatControllerTest.java`
- [X] T013 [P] [US1] Add failing client-contract cases for optional/null session IDs and repeated UI chat sends using one generated ID in `frontend/src/app/services/chat.service.spec.ts` and `frontend/src/app/chat/chat.component.spec.ts`
- [X] T014 [P] [US1] Add a failing two-request API regression with one synthetic session and complete response-field assertions in `tests/integration/test_chat_flow.sh`

### Implementation for User Story 1

- [X] T015 [US1] Implement stateful snapshot execution, stateless bypass, and success-only atomic `[UserMessage, AssistantMessage]` commit in `backend/src/main/java/com/example/chat/memory/ConversationMemoryService.java`
- [X] T016 [US1] Integrate the exact request `sessionId` with `ChatMemory` through `ConversationMemoryService` in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`; explicitly do not register `MessageChatMemoryAdvisor`, whose pre-call write violates failure non-retention
- [X] T017 [US1] Assemble provider messages from the current rendered system instruction, retained history, and current rendered user message while preserving request-scoped template variables and Ollama options in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`
- [X] T018 [US1] Preserve existing answer/usage/model/finish-reason mapping and ensure blank/provider failures do not commit memory in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`
- [X] T019 [P] [US1] Make `sessionId` optional and nullable in the HTTP client type while keeping the Angular component's generated stable ID in `frontend/src/app/services/chat.service.ts` and `frontend/src/app/chat/chat.component.ts`
- [X] T020 [US1] Update the browser acceptance flow to send two chat turns with the same identifier and assert unchanged response contracts without brittle semantic phrase matching in `tests/integration/frontend-chat-flow.spec.mjs`
- [X] T021 [US1] Run the US1 backend, Angular, shell integration, and Playwright tests referenced by `backend/build.gradle`, `frontend/package.json`, `tests/integration/test_chat_flow.sh`, and `tests/integration/frontend-chat-flow.spec.mjs`

**Checkpoint**: Same-session continuity works as an independently demonstrable MVP; stateless clients and all existing response fields still work.

---

## Phase 4: User Story 2 - Isolate and Bound Conversations (Priority: P2)

**Goal**: Exact identifiers remain isolated, each prompt and retained history contains at most 20 non-system messages, the current system instruction is always protected, failed calls never enter history, and overlapping same-session calls are deterministic.

**Independent Test**: Interleave exact-distinct IDs, exercise transient windows of 19/20/21 messages, change templates/sampling between turns, and use latches for overlapping calls; verify oldest-first eviction, current system first and outside the count, zero cross-session leakage, same-session serialization, different-session progress, and redacted logs.

### Tests for User Story 2 — write first and verify failure

- [X] T022 [P] [US2] Add failing 19/20/21 non-system prompt-window tests for current-user counting, maximum 20, chronological order, and strict oldest-first trimming in `backend/src/test/java/com/example/chat/memory/ConversationMemoryServiceTest.java`
- [X] T023 [US2] Add failing isolation cases for `A`, `B`, `Case`, `case`, and ` Case ` plus interleaved requests in `backend/src/test/java/com/example/chat/memory/ConversationMemoryServiceTest.java`
- [X] T024 [US2] Add latch-controlled failing concurrency tests proving same-ID serialization with prior-turn visibility and simultaneous progress for different IDs in `backend/src/test/java/com/example/chat/memory/ConversationMemoryServiceTest.java`
- [X] T025 [P] [US2] Add failing prompt-capture regressions proving the current system instruction is first, excluded from the 20-message count, never retained, and replaced by each request's template/context in `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`
- [X] T026 [P] [US2] Add failing regressions for explicit-zero/omitted sampling, feature 003 metrics, unknown properties, and stateless classification rejecting `sessionId` in `backend/src/test/java/com/example/chat/ChatControllerTest.java` and `backend/src/test/java/com/example/chat/ClassificationControllerTest.java`
- [X] T027 [P] [US2] Add failing log assertions using unique sentinel IDs/messages/answers/system text to prove no raw session ID, history, prompt, answer, or PII reaches logs in `backend/src/test/java/com/example/chat/filter/LogRedactionFilterTest.java`

### Implementation for User Story 2

- [X] T028 [US2] Bound `(retained history + current user)` to the newest 20 non-system messages before provider invocation in `backend/src/main/java/com/example/chat/memory/ConversationMemoryService.java`
- [X] T029 [US2] Add one fair exact-key admission gate per stateful session around snapshot → provider call → successful commit in `backend/src/main/java/com/example/chat/memory/ConversationMemoryService.java`
- [X] T030 [US2] Preserve independent execution for distinct IDs and stateless requests while preventing lost, duplicated, or reordered same-session commits in `backend/src/main/java/com/example/chat/memory/ConversationMemoryService.java`
- [X] T031 [US2] Keep every rendered system instruction transient and keep template/context/sampling state out of memory in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`
- [X] T032 [US2] Keep classification completely outside conversation memory and preserve its structured response/error flow in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java` and `backend/src/main/java/com/example/chat/ClassificationController.java`
- [X] T033 [US2] Ensure new memory coordination logs only safe operation/metric metadata and never identifiers or message content in `backend/src/main/java/com/example/chat/memory/ConversationMemoryService.java`, `backend/src/main/java/com/example/chat/ChatController.java`, and `backend/src/main/java/com/example/chat/filter/LogRedactionFilter.java`
- [X] T034 [US2] Extend the bounded API smoke test with same-ID continuity, exact-ID isolation, blank-ID rejection, metrics, templates, sampling, and classification regressions in `scripts/smoke_test.sh`; do not call `scripts/experiment_e.sh`
- [X] T035 [US2] Run all US2 memory, service, controller, classification, logging, smoke, and browser regressions from `backend/build.gradle`, `scripts/smoke_test.sh`, and `frontend/package.json`

**Checkpoint**: User Stories 1 and 2 satisfy the complete runtime memory contract with no cross-session, failure, template, sampling, metric, or logging regressions.

---

## Phase 5: User Story 3 - Reproduce Experiment E (Priority: P3)

**Goal**: A participant can explicitly run exactly 20 sequential synthetic turns, validate every established response envelope, and inspect continuity and eviction observations without making the experiment part of automated verification.

**Independent Test**: Invoke the script against healthy local services and verify one generated nonblank ID, exactly 20 sequential HTTP 200 turns, complete contract validation per turn, labeled early/recent-memory observations, a readable final summary, and a nonzero exit with the failing turn for simulated transport/HTTP/contract failures.

### Implementation and verification for User Story 3

- [X] T036 [US3] Create one generated synthetic session ID and an ordered array of exactly 20 fixed non-sensitive prompts with early and recent recall checkpoints in `scripts/experiment_e.sh`
- [X] T037 [US3] Implement strictly sequential curl calls, configurable `BASE_URL`, per-turn HTTP capture, and fail-fast turn-number diagnostics in `scripts/experiment_e.sh`
- [X] T038 [US3] Validate every response's request ID, nonblank answer, status, elapsed time, metric-field presence/nullability, model metadata fields, throughput field, and errors field with jq in `scripts/experiment_e.sh`
- [X] T039 [US3] Print per-turn outcomes and a final session/completed-turn/failure/recent-context/oldest-context summary without persisting a result file by default in `scripts/experiment_e.sh`
- [X] T040 [P] [US3] Align prerequisites, invocation, expected output, privacy guidance, and optional stdout redirection with the implemented flags in `specs/006-conversation-memory/quickstart.md`
- [X] T041 [US3] Validate `scripts/experiment_e.sh` with `bash -n`, then explicitly run it once against healthy local services and record only pass/fail plus completed-turn count in `specs/006-conversation-memory/quickstart.md`

**Checkpoint**: Experiment E is reproducible on demand and remains observational for model semantics.

---

## Phase 6: Polish and Cross-Cutting Verification

**Purpose**: Synchronize contracts and documentation, prove restart scope and experiment exclusion, then run the complete feature gate.

- [X] T042 [P] Synchronize implemented nullable/stateless/blank/exact-match session semantics and unchanged feature 003–005 response schemas in `specs/006-conversation-memory/contracts/chat-api.yaml`
- [X] T043 [P] Add or update the isolated application-context restart test proving a new backend context has no prior sessions in `backend/src/test/java/com/example/chat/memory/ConversationMemoryServiceTest.java`
- [X] T044 Audit all new and modified logging statements for identifier/history/PII leakage and add any missing sentinel assertions in `backend/src/main/java/com/example/chat/`, `backend/src/test/java/com/example/chat/filter/LogRedactionFilterTest.java`, and `backend/src/test/java/com/example/chat/memory/ConversationMemoryServiceTest.java`
- [X] T045 Verify that `scripts/experiment_e.sh` is absent from default Gradle, npm, smoke, Playwright, and CI execution paths in `backend/build.gradle`, `frontend/package.json`, `scripts/smoke_test.sh`, `tests/integration/`, and `.github/workflows/smoke-test.yml`
- [X] T046 Run the complete backend suite and Angular install/build/unit suite defined by `backend/build.gradle`, `frontend/package-lock.json`, and `frontend/package.json`
- [X] T047 Start `docker/docker-compose.yml`, run `scripts/smoke_test.sh` and `frontend` Playwright acceptance, then verify backend restart clears conversation memory without changing container topology or volumes
- [X] T048 Execute every non-experiment validation step in `specs/006-conversation-memory/quickstart.md`, confirm the OpenAPI YAML parses, and record final implementation-only pass/fail evidence without raw prompts, answers, identifiers, or experiment output

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 — Setup**: No dependencies.
- **Phase 2 — Foundation**: Depends on Phase 1 and blocks every user story.
- **Phase 3 — US1**: Depends on Phase 2; this is the MVP.
- **Phase 4 — US2**: Depends on the US1 memory path because it strengthens that path with bounds, isolation, concurrency, and privacy.
- **Phase 5 — US3**: Depends on US1 and US2 so the live run observes the final memory semantics.
- **Phase 6 — Polish**: Depends on all selected user stories.

### User Story Dependency Graph

```text
Setup → Foundation → US1 (continuity MVP) → US2 (isolation/window/concurrency) → US3 (experiment E) → Polish
```

### Within Each User Story

- Write the story's tests first and confirm they fail for the intended reason.
- Implement the smallest behavior that passes those tests.
- Run the focused story gate before modifying the next story.
- Do not install `MessageChatMemoryAdvisor`; `ConversationMemoryService` is the explicit success-aware memory integration boundary.
- Keep system prompts, template context, sampling options, error payloads, and experiment diagnostics out of stored conversation messages.

### Parallel Opportunities

- In Foundation, T004 and T006 can run in parallel before their corresponding implementation tasks.
- In US1, T010–T014 can run in parallel because they target separate test files; T019 can run alongside backend implementation after its tests exist.
- In US2, T022–T024 are sequential because they share `ConversationMemoryServiceTest.java`; T025–T027 can run in parallel. T031–T034 target different components after the memory coordinator is ready.
- In US3, T040 can run alongside T036–T039 and be reconciled before T041.
- In Polish, T042 and T043 can run in parallel before the final audit and full-suite gates.

---

## Parallel Examples

### User Story 1

```text
Task T010: Memory service same-session/stateless/failure tests
Task T011: Model-service prompt/template/sampling/metric tests
Task T012: Controller validation/error-contract tests
Task T013: Angular client/session tests
Task T014: Shell API two-turn regression
```

### User Story 2

```text
Task T022: Window-boundary tests
Task T023: Exact-key isolation tests
Task T024: Latch-controlled concurrency tests
Task T025: System-message/template tests
Task T026: Sampling/metrics/classification regressions
Task T027: Log-redaction sentinel tests
```

### User Story 3

```text
Task T036-T039: Implement the experiment script sequentially in scripts/experiment_e.sh
Task T040: Update quickstart documentation in parallel, then reconcile before T041
```

---

## Implementation Strategy

### MVP First — User Story 1

1. Complete Setup and Foundation.
2. Add failing US1 tests.
3. Implement explicit `ChatMemory` integration and exact conversation ID mapping without the stock advisor.
4. Preserve current templates, sampling options, metrics, errors, and classification behavior.
5. Stop and validate same-session continuity plus stateless compatibility.

### Incremental Delivery

1. **US1**: Deliver coherent same-session follow-ups as the MVP.
2. **US2**: Add exact isolation, strict windowing, same-session ordering, independent-session concurrency, and privacy proof.
3. **US3**: Add the explicit reproducible 20-turn experiment.
4. **Polish**: Synchronize the OpenAPI contract and run the complete local gate.

### Verification Discipline

- Use deterministic captured-message and latch tests for correctness; do not rely on model prose.
- Treat provider semantic output in experiment E as an observation, not a CI assertion.
- Preserve the one-call chat path and all feature 003–005 fields.
- Never print or persist raw session identifiers, history, system prompts, user text, or answers in application logs.

## Notes

- `[P]` marks tasks safe to execute in parallel; tasks touching the same file remain sequential.
- `[US1]`, `[US2]`, and `[US3]` provide requirement traceability.
- The stock `MessageChatMemoryAdvisor` is intentionally excluded because it writes the user message before provider success and requires a conversation ID even for stateless calls.
- `sessionId` is an opaque memory key, not an authenticated user identity; use only synthetic identifiers in tests and examples.
- Commit after each task or coherent test/implementation pair, following repository policy.
