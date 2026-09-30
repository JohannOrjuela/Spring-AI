# Implementation Plan: token-usage-metrics

**Branch**: `003-token-usage-metrics` | **Date**: 2026-09-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-token-usage-metrics/spec.md` plus implementation constraints supplied for planning.

## Summary

Extend the existing `POST /api/v1/chat` response with token usage and model completion metadata from the real Spring AI/Ollama `ChatResponse`, while preserving the existing response fields and request contract. Change the internal model-service result from plain text to a metadata-bearing result, calculate `elapsedMs` with `System.nanoTime`, calculate `tokensPerSecond` from completion tokens and the endpoint elapsed time, populate `null` for unavailable metrics, remove question/answer content from logs, and add deterministic unit/MockMvc coverage plus a live Ollama end-to-end check. No new endpoint, persistence, sampling controls, templates, memory, or later-block functionality is introduced.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.0, Spring AI 2.0.0 Ollama starter, Jakarta Validation, Spring Boot Test/JUnit 5/MockMvc

**Storage**: N/A; metrics are request-scoped response data and are not persisted

**Testing**: Gradle `test`, JUnit 5, Mockito-style service isolation where appropriate, Spring MockMvc endpoint tests, and the existing Docker/Ollama smoke flow executed locally and in CI

**Target Platform**: Dockerized local Linux containers with Java 21 backend; local developer execution supported

**Project Type**: Spring Boot web service with an Angular frontend and Docker Compose local model runtime

**Performance Goals**: Preserve the existing chat request path; calculate throughput without adding a model call or a second network request. `tokensPerSecond` must use `completionTokens / (elapsedMs / 1000)`, be rounded to two decimal places with a `+/-0.01` tolerance, and be `null` when the calculation is not valid.

**Constraints**: Keep `POST /api/v1/chat` and its request shape unchanged; preserve `requestId`, `answer`, `elapsedMs`, and `status`; include all new metric fields with `null` when unavailable; use the fully qualified Spring AI `org.springframework.ai.chat.model.ChatResponse` name to avoid collision with the project DTO; do not log questions, answers, tokenized text, or PII.

**Scale/Scope**: One existing endpoint, one existing DTO, one model-service implementation, existing error paths, focused unit/MockMvc tests, and CI smoke validation. No new service or endpoint.

## Constitution Check

*GATE: Explicitly evaluated before research and re-evaluated after design. A listed FAIL is not silently treated as PASS.*

| Principle | Baseline status | Planning decision / required gate |
|---|---|---|
| Demo-Focused Simplicity | PASS | Reuses the existing endpoint, DTO, service, and local model flow; adds no persistence or new service. |
| Reproducible Local Execution | PASS | Quickstart retains Docker Compose/Ollama execution and adds observable response assertions. |
| Clear Interfaces & Contracts | PASS with required compatibility tests | Contract remains `/api/v1/chat`; response is additive and documented in `contracts/chat-api.md`. |
| Data Minimization & Secrets Handling | **FAIL at baseline** | `ChatController` currently logs `question` and the serialized response, which can expose question/answer data. Implementation MUST remove content logging and tests MUST inspect logs for leakage before this gate passes. |
| Testing & Observability | **FAIL at baseline** | No backend test files currently exist, and CI currently runs only the smoke flow. Implementation MUST add deterministic service/DTO coverage, MockMvc endpoint/error coverage, log-redaction assertions, and a live Ollama end-to-end check in both local validation and CI before this gate passes. |

**Gate decision before Phase 0**: CONDITIONAL PROCEED. The two baseline failures are concrete remediation work required by this feature, not waived violations. No implementation plan task may mark the feature complete until both gates pass.

**Post-design re-check**: The design preserves the two mandatory remediation gates, adds no complexity exception, and defines tests for both. Final constitutional status remains **not yet passed** until implementation and validation demonstrate sanitized logs and complete-flow coverage.

## Project Structure

### Documentation (this feature)

```text
specs/003-token-usage-metrics/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── chat-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                    # created by /speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── build.gradle
├── src/main/java/com/example/chat/
│   ├── ChatController.java                 # elapsed time, response construction, safe logs
│   ├── ModelService.java                   # enriched internal result contract
│   ├── dto/ChatResponse.java               # additive JSON fields and Jackson constructor
│   ├── exception/GlobalExceptionHandler.java # complete error response shape
│   └── impl/SpringAiModelService.java      # call().chatResponse() metadata mapping
└── src/test/java/com/example/chat/
    ├── ChatControllerTest.java             # MockMvc success/validation/error contract
    ├── dto/ChatResponseTest.java           # constructor/accessor/serialization behavior
    └── impl/SpringAiModelServiceTest.java  # metadata mapping and unavailable values

frontend/
└── src/index.html                           # served chat UI and contract display

scripts/
└── smoke_test.sh                           # end-to-end response assertions

.github/workflows/
└── smoke-test.yml                           # Gradle and Docker smoke gates
```

**Structure Decision**: Keep the existing single backend module and Docker Compose topology. Add focused tests under the existing Gradle test source set; do not create a metrics service, repository, or endpoint.

## Complexity Tracking

No constitution violation is being introduced. The two baseline gate failures are explicitly tracked as required remediation for this feature and are not accepted as permanent exceptions.

| Baseline issue | Why it is in scope | Required resolution |
|---|---|---|
| Question/answer content currently appears in controller logs | The feature requires privacy-safe observability | Log only non-sensitive correlation, status, elapsed time, and aggregate metric values; add log assertions. |
| Backend has no existing tests | The feature changes a public response contract and metadata mapping | Add unit, MockMvc, and live Ollama flow coverage before final sign-off. |
