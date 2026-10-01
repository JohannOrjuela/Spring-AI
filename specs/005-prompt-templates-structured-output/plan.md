# Implementation Plan: Prompt Templates and Structured Output

**Branch**: `005-prompt-templates-structured-output` | **Date**: 2026-09-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-prompt-templates-structured-output/spec.md` and the implementation constraints supplied for planning.

## Summary

Replace the hard-coded system prompt with three server-registered classpath templates (`conciso`, `tutor`, and `extractor`) rendered with controlled request variables and `<...>` delimiters. Extend chat requests with template selection and context while retaining all feature 003 metrics and feature 004 sampling semantics. Add `POST /api/v1/classifications`, using provider-native structured output plus schema validation and Spring AI's typed response wrapper so one final response supplies both `Clasificacion` and model metadata. Convert the currently deployed single-file frontend into the Angular 20 application required by the constitution, preserving its existing sampling, health, metrics, and Playwright behavior before adding the two feature flows; do not automate or report the workshop experiments.

## Technical Context

**Language/Version**: Java 21; TypeScript in the planned Angular 20 frontend migrated from the currently served inline page

**Primary Dependencies**: Spring Boot 4.1.0, Spring AI 2.0.0 Ollama starter, Spring AI `StTemplateRenderer`, Jakarta Bean Validation, Jackson annotations, JUnit 5, Mockito, MockMvc, Angular 20, Angular CLI/build tooling, Angular unit-test tooling, Playwright

**Storage**: Read-only classpath prompt resources; no database, prompt persistence, request persistence, or mutable global prompt configuration

**Testing**: Gradle `test`; deterministic unit and MockMvc tests; Angular unit tests through the configured `npm test` script; Docker/Ollama API smoke checks; Playwright frontend-to-backend-to-model acceptance

**Target Platform**: Dockerized local Linux containers with Java 21 backend, Node 24 Angular frontend, and the existing Ollama `gemma3:4b` runtime; Windows and Unix development supported

**Project Type**: Versioned Spring Boot HTTP service plus Angular browser client and Docker Compose local-model runtime

**Performance Goals**: Ordinary chat remains one provider call; a valid structured response completes in one provider call, while schema validation may use Spring AI's bounded correction attempts only when output is invalid; invalid identifiers and input fail before model work

**Constraints**: Exact allowlist lookup only; reject every unknown request property; no client prompt text or resource path resolution; request-scoped rendering and options; `<`/`>` delimiters; `text` maps to the common `question` variable; `Clasificacion` is a record whose nullable-capable `Integer confianza` is required with `@NotNull`; explicit Bean Validation of converted classifications; generated nonblank request IDs on every error; final-response metrics retained; raw prompts, questions, answers, classification text, and rejected values excluded from logs; the Docker-served frontend MUST be the Angular build output rather than the legacy inline script

**Scale/Scope**: Two versioned endpoints, three registered templates, one classification schema, one existing frontend page, no new infrastructure, persistence, experiment runner, result table, graph, or cost analysis

## Constitution Check

*GATE: Explicitly evaluated before research and re-evaluated after design. A listed baseline failure is implementation work, not an accepted exception.*

| Principle | Baseline status | Planning decision / required gate |
|---|---|---|
| Demo-Focused Simplicity | PASS BY DESIGN | Reuses the current backend, Ollama runtime, DTO conventions, metrics, and validation shape. The currently served single-file page is migrated into the already-started Angular component structure rather than maintaining two frontends; no service or datastore is added. |
| Reproducible Local Execution | PASS BY DESIGN | Existing Docker Compose topology and model remain unchanged. The frontend package MUST gain a complete Angular 20 build/test configuration, and Docker MUST serve its build output. Quickstart MUST document `npm ci`, unit/build/E2E commands, Compose environment variables, practical minimum hardware, optional GPU behavior, and model size/provenance/license. |
| Clear Interfaces & Contracts | **FAIL at baseline** | The current contract lacks template fields and a classification endpoint. Implementation MUST add and test the OpenAPI contract and examples before this gate passes. |
| Data Minimization & Secrets Handling | **FAIL for this feature at baseline** | Current code has no template/context flow. Implementation MUST prove that identifiers are allowlisted and that rendered prompts, context, questions, answers, classification content, and rejected values are not logged. |
| Testing & Observability | **FAIL for this feature at baseline** | No tests cover template selection, rendering, schema validation, classification metrics, or the two browser flows. Implementation MUST add unit, controller, smoke, and Playwright coverage while retaining feature 003/004 regression tests. |

**Gate decision before Phase 0**: CONDITIONAL PROCEED. The three failures identify missing feature artifacts and tests that this plan explicitly supplies; no constitutional exception is requested.

**Post-design re-check**: PASS BY DESIGN. The API contract defines both flows, the registry prevents arbitrary prompt control, converted classifications receive explicit application validation, all errors receive request identifiers, logs remain content-free, Docker topology is unchanged, and the plan explicitly activates, tests, builds, and deploys Angular before exercising browser acceptance. Final passage remains contingent on implementation, complete environment/hardware/model documentation, and passing verification.

## Project Structure

### Documentation (this feature)

```text
specs/005-prompt-templates-structured-output/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── chat-api.yaml
├── checklists/
│   └── requirements.md
└── tasks.md                         # created by /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/
├── java/com/example/chat/
│   ├── ChatController.java
│   ├── ClassificationController.java
│   ├── ModelService.java
│   ├── dto/
│   │   ├── SamplingParameters.java
│   │   ├── ChatRequest.java
│   │   ├── ChatResponse.java
│   │   ├── ClassificationRequest.java
│   │   ├── Clasificacion.java
│   │   └── ClassificationResponse.java
│   ├── prompt/
│   │   └── PromptTemplateRegistry.java
│   └── impl/
│       └── SpringAiModelService.java
└── resources/prompts/
    ├── conciso.st
    ├── tutor.st
    ├── extractor.st
    └── user.st

backend/src/test/java/com/example/chat/
├── ChatControllerTest.java
├── ClassificationControllerTest.java
├── dto/
│   ├── ChatRequestTest.java
│   ├── ClassificationRequestTest.java
│   └── ClasificacionTest.java
├── exception/GlobalExceptionHandlerTest.java
├── filter/LogRedactionFilterTest.java
├── prompt/PromptTemplateRegistryTest.java
└── impl/SpringAiModelServiceTest.java

frontend/src/app/
├── app.component.ts
├── app.component.html
├── app.config.ts
├── chat/
│   ├── chat.component.ts                       # template/context and chat/classify controls
│   ├── chat.component.html
│   ├── chat.component.css
│   └── chat.component.spec.ts
└── services/
    ├── chat.service.ts                         # typed chat and classification API client
    └── chat.service.spec.ts
frontend/src/main.ts                            # Angular bootstrap
frontend/src/index.html                         # Angular host only; no inline application script
frontend/angular.json
frontend/tsconfig.json
frontend/tsconfig.app.json
frontend/tsconfig.spec.json
frontend/package.json                           # Angular build, unit, and Playwright scripts
frontend/Dockerfile                             # multi-stage Angular build and static serving
tests/integration/frontend-chat-flow.spec.mjs  # real chat and classification browser flows
scripts/smoke_test.sh                           # template, rejection, classification assertions
.github/workflows/smoke-test.yml                # existing full-flow CI gate
```

**Structure Decision**: Keep a single backend and single browser page. The current Docker image serves only `frontend/src/index.html`, while partial Angular component files are not bootstrapped or built. Foundational work therefore creates one real Angular 20 application, ports the existing health, session, sampling, metrics, and safe-text rendering behavior into it, and changes Docker to serve `dist/`; the legacy inline application script is removed so there is only one frontend implementation. Extract the five shared sampling properties to a validated base DTO so both flat JSON requests reuse the exact feature 004 rules. The chat endpoint receives template selection and context; the dedicated classification request receives text, the same optional context, and sampling, but no template identifier because it always uses the registered extractor behavior. A closed registry maps exact public identifiers to fixed classpath resources. `SpringAiModelService` remains the only model boundary and returns domain-plus-metadata records to controllers.

## Complexity Tracking

No constitutional violation or complexity exception is introduced. The dedicated classification controller and response type are necessary to keep the free-form and structured contracts unambiguous; all other runtime topology remains unchanged.
