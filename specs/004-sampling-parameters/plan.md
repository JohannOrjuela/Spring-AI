# Implementation Plan: Per-Request Sampling Parameters

**Branch**: `004-sampling-parameters` | **Date**: 2026-09-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-sampling-parameters/spec.md` and the implementation constraints supplied for planning.

## Summary

Extend the existing `POST /api/v1/chat` request with nullable wrapper fields for `temperature`, `topP`, `topK`, `numPredict`, and `seed`. Validate the four bounded fields with Jakarta Bean Validation, forward only explicitly supplied values as request-scoped `OllamaChatOptions`, preserve configured Ollama defaults for omitted or null fields, and retain the complete feature 003 response and metric-nullability rules. Invalid input returns HTTP 400 before `ModelService` is invoked and includes safe field-level details without logging submitted values.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.0, Spring AI 2.0.0 Ollama starter, Jakarta Bean Validation, Spring Boot Test/JUnit 5/MockMvc

**Storage**: N/A; sampling values are request-scoped and are neither persisted nor written to global configuration

**Testing**: Gradle `test`, JUnit 5, Mockito, MockMvc, Docker/Ollama smoke checks, and a Playwright browser regression of the existing frontend chat flow

**Target Platform**: Dockerized local Linux containers with Java 21 backend; Windows and Unix local development supported

**Project Type**: Spring Boot web service with an Angular frontend and Docker Compose local model runtime

**Performance Goals**: Apply request options in the existing single model call with no extra network request; reject invalid input before any model work begins

**Constraints**: Preserve configured defaults for null/omitted fields; preserve explicit `0`/`0.0`; use `OllamaChatOptions`, not deprecated `OllamaOptions`; keep feature 003 fields and nullability unchanged; do not persist or log request content or rejected values

**Scale/Scope**: One existing endpoint, one request DTO, one service boundary and implementation, one additive validation-error collection, focused backend tests, and one browser regression of the unchanged frontend; no new frontend sampling controls are required

## Constitution Check

*GATE: Explicitly evaluated before research and re-evaluated after design. A listed FAIL is not silently treated as PASS.*

| Principle | Baseline status | Planning decision / required gate |
|---|---|---|
| Demo-Focused Simplicity | PASS | Reuses the current endpoint and service; adds no endpoint, storage, infrastructure, or separate configuration service. |
| Reproducible Local Execution | PASS | Existing Docker Compose topology and global defaults remain unchanged; Quickstart adds request examples and verification commands. |
| Clear Interfaces & Contracts | **FAIL at baseline** | The request lacks the five fields and validation errors lack field-level details. Implementation MUST update and test the versioned JSON contract before this gate passes. |
| Data Minimization & Secrets Handling | **FAIL at baseline** | Validation currently logs `BindingResult.getAllErrors()`, which can include rejected values. Implementation MUST log only safe field names/error codes and add a non-disclosure assertion. |
| Testing & Observability | **FAIL for this feature at baseline** | Feature 003 has backend coverage, but no tests cover request-scoped options, omission, zero values, bounds, rejection-before-model, concurrent isolation, or the constitution's complete frontend-to-model flow. Implementation MUST add deterministic service/MockMvc coverage, extend the live smoke path, and add a browser regression of the existing frontend. |

**Gate decision before Phase 0**: CONDITIONAL PROCEED. The failures are existing gaps that this feature must remediate; none is accepted as a permanent exception.

**Post-design re-check**: The contract, data model, test strategy, safe-log rule, and Quickstart explicitly resolve every baseline gap without introducing extra infrastructure. The design is constitution-compliant; final passage remains contingent on implementation and passing validation.

## Project Structure

### Documentation (this feature)

```text
specs/004-sampling-parameters/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── chat-api.yaml
├── checklists/
│   └── requirements.md
└── tasks.md                    # dependency-ordered implementation tasks
```

### Source Code (repository root)

```text
backend/
├── build.gradle
├── src/main/java/com/example/chat/
│   ├── ChatController.java                  # validation response and request forwarding
│   ├── ModelService.java                    # request-aware service boundary
│   ├── dto/ChatRequest.java                 # nullable fields and Bean Validation
│   ├── dto/ChatResponse.java                # existing metrics plus validation errors
│   └── impl/SpringAiModelService.java       # conditional OllamaChatOptions
└── src/test/java/com/example/chat/
    ├── ChatControllerTest.java              # HTTP contract and no-model rejection
    ├── dto/ChatRequestTest.java              # boundaries and null/zero semantics
    └── impl/SpringAiModelServiceTest.java   # option mapping and metadata preservation

scripts/
└── smoke_test.sh                            # live valid and invalid request assertions

frontend/
├── package.json                             # browser-test script and Playwright test dependency
└── package-lock.json                        # reproducible browser-test dependency lock

tests/integration/
├── test_chat_flow.sh                        # existing endpoint integration check
└── frontend-chat-flow.spec.mjs             # frontend → backend → Ollama regression

.github/workflows/
└── smoke-test.yml                           # backend, API smoke, and browser acceptance gates
```

**Structure Decision**: Keep the existing single backend module and endpoint. Pass the validated `ChatRequest` through the existing `ModelService` boundary so the implementation can construct per-request options without adding a second parameter object or service. The frontend behavior remains unchanged, but a Playwright regression exercises its existing form through the real backend/model path to satisfy the constitutional end-to-end gate.

## Complexity Tracking

No constitutional violation or complexity exception is introduced. The baseline failures above are mandatory implementation work, not justified deviations.
