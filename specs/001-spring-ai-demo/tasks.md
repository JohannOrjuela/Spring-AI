---

description: "Task list generated for spring-ai-demo"

---

# Tasks: spring-ai-demo

**Input**: plan.md, spec.md, data-model.md, contracts/chat-api.md, quickstart.md

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Create repo directories and standard layout (backend/, frontend/, docker/, scripts/) — path: ./
- [x] T002 Initialize Java 25 Spring Boot backend skeleton in backend/ (Gradle wrapper, basic module) — path: backend/
- [x] T003 Initialize Angular 20 frontend skeleton in frontend/ (npm scaffold, base component) — path: frontend/
- [x] T004 Add docker-compose.yml skeleton and docker/ directory for model + services — path: docker/docker-compose.yml
- [x] T005 Add .env.example with required environment variables and placeholders — path: .env.example
- [x] T006 Add Quickstart file (validate and finalize quickstart.md) — path: specs/001-spring-ai-demo/quickstart.md
- [x] T007 Configure linters and formatters for backend and frontend (e.g., checkstyle/spotless, eslint/formatter) — path: backend/, frontend/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

- [x] T008 [P] Provide Docker image configuration for local NVIDIA model container and readiness probe (docker/model/README.md) — path: docker/model/
- [x] T009 [P] Implement backend chat API skeleton (ChatController + DTOs) — path: backend/src/main/java/com/example/chat/ChatController.java
- [x] T010 [P] Implement Spring AI integration service skeleton (ModelService) with pluggable model client interface — path: backend/src/main/java/com/example/chat/ModelService.java
- [x] T011 [P] Add smoke test script to scripts/smoke_test.sh that hits /api/v1/chat and asserts non-empty answer — path: scripts/smoke_test.sh
- [x] T012 [P] Implement server-side log redaction filter (redacts PII/secrets) — path: backend/src/main/java/com/example/chat/LogRedactionFilter.java
- [x] T013 [P] Add CI job skeleton to run smoke test (CI config file) — path: .github/workflows/smoke-test.yml

**Checkpoint**: Foundation ready - user story implementation can begin

---

## Phase 3: User Story 1 - Chat demo (Priority: P1) 🎯 MVP

**Goal**: End-to-end chat demo: Angular UI → Spring Boot API → local model → UI

**Independent Test**: Quickstart + scripts/smoke_test.sh returns success

- [x] T014 [US1] Create Angular ChatComponent with message input and display area — path: frontend/src/app/chat/chat.component.ts
- [x] T015 [US1] Implement Frontend ChatService to POST to /api/v1/chat and render responses — path: frontend/src/app/services/chat.service.ts
- [x] T016 [US1] Implement backend ChatController endpoint POST /api/v1/chat (maps to ModelService) — path: backend/src/main/java/com/example/chat/ChatController.java
- [x] T017 [US1] Implement ModelService call to Spring AI client with pluggable adapter for local NVIDIA model — path: backend/src/main/java/com/example/chat/ModelService.java
- [x] T018 [US1] Add integration smoke test that runs frontend (or simulates request) and backend and asserts model response — path: tests/integration/test_chat_flow.sh
- [x] T019 [US1] Add example request/response fixtures to contracts/chat-api.md (update with concrete examples) — path: specs/001-spring-ai-demo/contracts/chat-api.md

**Checkpoint**: US1 independently functional and testable

---

## Phase 4: User Story 2 - Local reproducibility (Priority: P2)

**Goal**: Ensure developers can reproduce the demo locally with documented commands

**Independent Test**: Follow Quickstart and confirm smoke test passes

- [x] T020 [US2] Finalize Quickstart commands and scripts for local startup (ensure docker-compose up runs model, backend, frontend) — path: specs/001-spring-ai-demo/quickstart.md
- [x] T021 [US2] Add sample .env file with placeholders and documented variables — path: .env.example
- [ ] T022 [US2] Add docker-compose.override.yml for developer convenience (volumes, live reload) — path: docker/docker-compose.override.yml
- [ ] T023 [US2] Provide checklist and manual validation steps in docs/README-validate.md — path: docs/README-validate.md
- [x] T024 [US2] Create a small CI step to run smoke_test.sh on merges to main (if CI available) — path: .github/workflows/smoke-test.yml

**Checkpoint**: US2 reproducibility validated

---

## Phase 5: User Story 3 - Safe logging and secrets (Priority: P3)

**Goal**: Avoid accidental secret/PII leakage in logs and examples

**Independent Test**: Run smoke test and inspect logs for redacted values

- [x] T025 [US3] Implement documentation for secrets handling and example env instructions — path: docs/secrets.md
- [x] T026 [US3] Implement backend log redaction unit tests verifying patterns are removed — path: backend/src/test/java/com/example/chat/LogRedactionFilterTest.java
- [x] T027 [US3] Add pre-commit hook to prevent committing files matching common secret patterns (hook script) — path: .githooks/pre-commit
- [x] T028 [US3] Add acceptance check in smoke_test.sh to scan logs for obvious secrets/PII patterns — path: scripts/smoke_test.sh

**Checkpoint**: US3 verified

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T029 Update README.md with architecture diagram and troubleshooting tips — path: README.md
- [ ] T030 Instrument simple metrics (MODEL_UP, REQUEST_SUCCESS, REQUEST_LATENCY_MS) in backend — path: backend/src/main/java/com/example/chat/MetricsConfig.java
- [ ] T031 Add basic end-to-end demo recording instructions (how to capture screenshots/video) — path: docs/demo-recording.md
- [ ] T032 Code cleanup, formatting, and finalize contributor docs — path: CONTRIBUTING.md

---

## Dependencies & Execution Order

- Setup (Phase 1) → Foundational (Phase 2) → User Stories (Phase 3+) → Polish
- User Story 1 (P1) should be implemented first as MVP

## Task Counts & Parallel Opportunities

- Total tasks: 32
- Tasks marked [P] are parallelizable: T008, T009, T010, T011, T012, T013 (and others marked [P] where indicated)

## Implementation Strategy

- MVP First: Complete Phase 1 + Phase 2 → implement US1 for demo → validate smoke test → add US2 reproducibility tasks → finalize US3 security tasks

---

## Notes

- Tests were included where the spec requested an automated smoke test; full unit test suites are left to implementation phase.

