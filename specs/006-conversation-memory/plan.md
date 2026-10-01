# Implementation Plan: Conversation Memory

**Branch**: `006-conversation-memory` | **Date**: 2026-10-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-conversation-memory/spec.md`

## Summary

Add process-local conversation memory to `POST /api/v1/chat` without changing its successful response shape. A request with a nonblank `sessionId` is coordinated under an exact, case-sensitive per-session gate; the current registered system prompt remains request-scoped and outside memory, while the newest user/assistant messages plus the current user message are limited to 20. The completed user/assistant pair is committed only after a nonblank model answer. Null or omitted identifiers stay stateless, classification stays stateless, and all feature 003–005 template, sampling, metric, validation, privacy, and error behavior remains intact. Add a manually invoked Bash experiment E runner for exactly 20 sequential turns; it is never part of the default build, smoke test, or CI.

## Technical Context

**Language/Version**: Java 21; TypeScript 5.9 in the existing Angular 20 frontend; Bash for the explicit experiment runner

**Primary Dependencies**: Spring Boot 4.1.0, Spring AI 2.0.0 `ChatClient`, Spring AI message and `MessageWindowChatMemory` APIs already supplied by the Ollama starter, Jakarta Bean Validation, JUnit 5, Mockito, MockMvc, Angular 20, Playwright, curl, and jq

**Storage**: Process-local `MessageWindowChatMemory` backed by Spring AI's in-memory repository, containing only user and assistant messages; no database, external cache, disk persistence, system-message persistence, TTL, or cross-restart recovery

**Testing**: Gradle `test`; deterministic memory-window, service, DTO, controller, logging, failure, and latch-controlled concurrency tests; Angular unit tests; existing Docker/Ollama smoke and Playwright acceptance; experiment E remains an explicit manual live-model run

**Target Platform**: Existing Dockerized local Linux containers with Java 21 backend, Node 24 Angular frontend, and Ollama `gemma3:4b`; Windows and Unix development remain supported

**Project Type**: Versioned Spring Boot HTTP service plus Angular browser client and Docker Compose local-model runtime

**Performance Goals**: One provider call per accepted chat request; no provider call for invalid input; same-session calls serialize for deterministic context while different identifiers and stateless calls remain independently executable; prompt context never exceeds 20 non-system messages plus the current system instruction

**Constraints**: Exact case-sensitive `sessionId` keys with no trimming; omitted/null remains stateless and blank is HTTP 400; current user message counts in the 20-message prompt window; only nonblank successful user/assistant pairs commit; current rendered system instruction is first and never stored; no advisor that writes before provider success; no raw identifiers or conversation content in logs; request-scoped template/context/sampling behavior and all feature 003–005 response/error fields remain unchanged

**Scale/Scope**: One backend instance, one existing browser flow, up to 20 retained messages per active identifier, demo-scale active-session cardinality with no expiry or management API, no new service or volume, one explicit 20-turn experiment script

## Constitution Check

*GATE: Explicitly evaluated before research and re-evaluated after design. A baseline gap is implementation work, not an accepted exception.*

| Principle | Baseline status | Planning decision / required gate |
|---|---|---|
| Demo-Focused Simplicity | PASS BY DESIGN | Reuse the existing backend, frontend, local model, request DTO, prompt registry, and Spring AI dependencies. Add only one in-process memory coordinator and no database, cache, endpoint, or UI workflow. |
| Reproducible Local Execution | PASS BY DESIGN | Docker topology and model remain unchanged. Quickstart adds deterministic checks and a separate explicit experiment command with its Bash/curl/jq prerequisites. |
| Clear Interfaces & Contracts | **FAIL for this feature at baseline** | The current contract treats `sessionId` as inert and permits blanks. Implementation MUST document stateful/null/blank/exact-match semantics and preserve both response envelopes. |
| Data Minimization & Secrets Handling | **FAIL for this feature at baseline** | Runtime message retention is new. Implementation MUST keep it process-local, omit system prompts and request settings from memory, avoid logging identifiers/content, and prove restart clearing and failure non-retention. |
| Testing & Observability | **FAIL for this feature at baseline** | No tests cover recall, isolation, eviction, restart scope, or same-session concurrency. Implementation MUST add deterministic unit/controller/concurrency coverage and extend the existing end-to-end chat regression without placing experiment E in CI. |

**Gate decision before Phase 0**: CONDITIONAL PROCEED. The three failures identify the contract, privacy, and verification artifacts this plan supplies; no constitutional exception is requested.

**Post-design re-check**: PASS BY DESIGN. The OpenAPI contract fixes the identifier semantics without changing response shapes, the data model stores only bounded non-system content in process, the memory coordinator commits only successful turns under per-session serialization, the quickstart covers local/restart behavior, and the test plan preserves all 003–005 flows. Final passage remains contingent on implementation and passing verification.

## Project Structure

### Documentation (this feature)

```text
specs/006-conversation-memory/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── chat-api.yaml
├── checklists/
│   └── requirements.md
└── tasks.md                         # created later by /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/example/chat/
├── ChatController.java                         # unchanged response mapping; validates sessionId
├── ModelService.java                           # existing boundary retained
├── dto/
│   └── ChatRequest.java                        # nullable-but-nonblank sessionId validation
├── impl/
│   └── SpringAiModelService.java               # stateful/stateless prompt assembly and commit
└── memory/
    ├── ConversationMemoryConfiguration.java    # in-memory ChatMemory, max 20
    └── ConversationMemoryService.java          # exact-key gates, bounded prompt, success-only commit

backend/src/test/java/com/example/chat/
├── ChatControllerTest.java
├── dto/ChatRequestTest.java
├── impl/SpringAiModelServiceTest.java
├── filter/LogRedactionFilterTest.java
└── memory/ConversationMemoryServiceTest.java

frontend/src/app/
├── chat/chat.component.ts                      # existing stable UI session reused
├── chat/chat.component.spec.ts
└── services/
    ├── chat.service.ts                         # contract permits omitted/null sessionId
    └── chat.service.spec.ts

scripts/
├── smoke_test.sh                               # bounded regression only; never experiment E
└── experiment_e.sh                             # explicit 20-turn synthetic run

tests/integration/
├── frontend-chat-flow.spec.mjs                 # two-turn same-session regression
└── test_chat_flow.sh                            # API-level stateful/stateless regression

.github/workflows/smoke-test.yml                 # preserves normal gates; excludes experiment E
docker/docker-compose.yml                        # unchanged topology and volumes
```

**Structure Decision**: Keep the existing backend/frontend split and current versioned endpoints. Add a small backend memory package instead of embedding mutable state in the controller or prompt registry. `SpringAiModelService` continues to own provider interaction and metadata mapping; it delegates session ordering, bounded history selection, and success-only commit to the memory service. The Angular component already generates and reuses one identifier, so no new UI is required. Classification, prompt resources, Docker topology, and response DTOs remain unchanged.

## Complexity Tracking

No constitutional violation or complexity exception is introduced. Per-session coordination is required by the deterministic ordering contract; the process-local implementation is the smallest design that also prevents failed calls and concurrent updates from contaminating memory.

