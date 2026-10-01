# Tasks: Prompt Templates and Structured Output

**Input**: Design documents from `/specs/005-prompt-templates-structured-output/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/chat-api.yaml`, `quickstart.md`

**Tests**: Automated tests are required by the specification and constitution. Write the story tests first and confirm they fail for the expected missing behavior before implementing that story.

**Organization**: Tasks are grouped by user story so template selection, structured classification, and safe compatibility can be implemented and verified as distinct increments.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it changes a different file and does not depend on another incomplete task in the same group.
- **[Story]**: Maps the task to User Story 1, 2, or 3 from `spec.md`.
- Every task names the exact file or files it changes or verifies.

## Phase 1: Setup and Baseline Review

**Purpose**: Understand the current feature 003/004 behavior before changing shared contracts.

- [X] T001 Review the hard-coded prompt, `ChatRequest`, controller contract, sampling construction, metrics mapping, backend tests, deployed inline frontend, inactive Angular files, Docker serving path, and frontend scripts in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`, `backend/src/main/java/com/example/chat/dto/ChatRequest.java`, `backend/src/main/java/com/example/chat/ChatController.java`, `backend/src/main/java/com/example/chat/ModelService.java`, `backend/src/test/java/com/example/chat/`, `frontend/src/index.html`, `frontend/src/app/`, `frontend/Dockerfile`, and `frontend/package.json`

---

## Phase 2: Foundational Request Model and Angular Runtime

**Purpose**: Create the shared flat sampling contract and make the constitution-required Angular application the frontend actually built and served by Docker.

**⚠️ CRITICAL**: Complete this phase before implementing either endpoint.

- [X] T002 Extract the five wrapper-typed sampling fields, existing Bean Validation ranges, getters, and setters into `backend/src/main/java/com/example/chat/dto/SamplingParameters.java`, then make `backend/src/main/java/com/example/chat/dto/ChatRequest.java` inherit them without changing its flat JSON shape
- [X] T003 Configure Angular 20 runtime, build, unit-test, and TypeScript dependencies plus `start`, `build`, `test`, and `test:e2e` scripts in `frontend/package.json`, regenerate `frontend/package-lock.json`, and create `frontend/angular.json`, `frontend/tsconfig.json`, `frontend/tsconfig.app.json`, and `frontend/tsconfig.spec.json`
- [X] T004 Add failing baseline Angular tests for the existing feature 003/004 request payload, explicit-zero sampling values, health state, safe response rendering, and metric fields in `frontend/src/app/chat/chat.component.spec.ts` and `frontend/src/app/services/chat.service.spec.ts`
- [X] T005 Bootstrap the standalone Angular application with `provideHttpClient`, root composition, and the chat component in `frontend/src/main.ts`, `frontend/src/app/app.config.ts`, `frontend/src/app/app.component.ts`, and `frontend/src/app/app.component.html`
- [X] T006 Port the currently deployed health probe, per-tab session ID, sampling controls, request timeout, safe text rendering, response/error envelope, and all feature 003 metrics from the inline script into `frontend/src/app/chat/chat.component.ts`, `frontend/src/app/chat/chat.component.html`, `frontend/src/app/chat/chat.component.css`, and `frontend/src/app/services/chat.service.ts`; reduce `frontend/src/index.html` to the Angular host document
- [X] T007 Update `frontend/Dockerfile` to install locked dependencies, build Angular, and serve the generated `dist/` application on port 4200 instead of copying the legacy `frontend/src/index.html` directly
- [X] T008 Run `npm ci`, `npm run build`, and `npm test -- --watch=false` from `frontend/package.json`, then verify the built application is the artifact copied by `frontend/Dockerfile`

**Checkpoint**: Chat still exposes `temperature`, `topP`, `topK`, `numPredict`, and `seed` at the top level; Angular unit tests preserve feature 003/004 behavior; and Docker serves the Angular build rather than the legacy inline script.

---

## Phase 3: User Story 1 — Select a Registered Prompt Template (Priority: P1) 🎯 MVP

**Goal**: Let chat clients select `conciso`, `tutor`, or `extractor`, pass controlled context, and use `conciso` by default without accepting prompt text or resource paths.

**Independent Test**: Send otherwise identical chat requests with each registered identifier, omitted/null `templateId`, and controlled context; verify the selected resource is rendered for only that request, literal JSON braces survive, and sampling plus metrics remain unchanged.

### Tests for User Story 1

- [X] T009 [P] [US1] Add failing DTO tests proving omitted/null `templateId`, `rol`, `dominio`, and `idioma` remain null before prompt construction, explicitly blank context is invalid, and nonblank long context is accepted in `backend/src/test/java/com/example/chat/dto/ChatRequestTest.java`
- [X] T010 [P] [US1] Add failing registry and rendering tests for all three exact identifiers, the `conciso` default, controlled-variable substitution, literal JSON braces, missing variables, and request isolation in `backend/src/test/java/com/example/chat/prompt/PromptTemplateRegistryTest.java`
- [X] T011 [P] [US1] Add failing model-service tests for selecting each template, applying `<`/`>` rendering, preserving `question` as user content, retaining `sessionId`, forwarding feature 004 sampling options, and mapping feature 003 metrics in `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`
- [X] T012 [P] [US1] Add failing MockMvc tests for registered and omitted/null template identifiers, request-time context defaults, and the unchanged chat response envelope in `backend/src/test/java/com/example/chat/ChatControllerTest.java`

### Implementation for User Story 1

- [X] T013 [P] [US1] Add optional `templateId`, `rol`, `dominio`, and `idioma` fields with getters, setters, blank-value validation, null-preserving deserialization, and strict unknown-property rejection in `backend/src/main/java/com/example/chat/dto/ChatRequest.java`
- [X] T014 [P] [US1] Create editable `<`/`>` templates for concise, tutor, extractor, and common user content in `backend/src/main/resources/prompts/conciso.st`, `backend/src/main/resources/prompts/tutor.st`, `backend/src/main/resources/prompts/extractor.st`, and `backend/src/main/resources/prompts/user.st`, including a literal JSON example that keeps braces unchanged
- [X] T015 [US1] Implement an immutable exact-match allowlist from `conciso`, `tutor`, and `extractor` to fixed `ClassPathResource` values plus a shared `StTemplateRenderer` configured with `<` and `>` in `backend/src/main/java/com/example/chat/prompt/PromptTemplateRegistry.java`
- [X] T016 [US1] Replace the hard-coded default system prompt with request-scoped registry resolution, normalize null context only at prompt construction, and render controlled maps containing only `rol`, `dominio`, `idioma`, and `question` in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`, while retaining `OllamaChatOptions` and metadata mapping
- [X] T017 [US1] Wire the extended request through the existing `POST /api/v1/chat` flow without changing `question`, `sessionId`, HTTP success shape, or feature 003 metric nullability in `backend/src/main/java/com/example/chat/ChatController.java` and `backend/src/main/java/com/example/chat/ModelService.java`
- [X] T018 [P] [US1] Extend the typed chat request payload and service method with registered template and context fields while preserving sampling fields in `frontend/src/app/services/chat.service.ts`
- [X] T019 [US1] Add the registered-template selector and optional role, domain, and language controls, with no free-form system-prompt or path field, in `frontend/src/app/chat/chat.component.ts`, `frontend/src/app/chat/chat.component.html`, and `frontend/src/app/chat/chat.component.css`

**Checkpoint**: User Story 1 works independently: registered templates and defaults can be exercised through chat, and existing sampling/metrics remain visible.

---

## Phase 4: User Story 2 — Receive a Structured Classification (Priority: P2)

**Goal**: Provide `POST /api/v1/classifications` with a required typed `Clasificacion`, existing sampling controls, controlled context, schema enforcement, and feature 003 metrics.

**Independent Test**: Submit valid text and sampling values; verify HTTP 200 contains nonblank `categoria`, integer `confianza` from 0 to 100, nonblank `justificacion`, and all existing metric fields. Simulate invalid structured output and verify a safe error instead of partial success.

### Tests for User Story 2

- [X] T020 [P] [US2] Add failing validation tests for required `text`, null-preserving optional context, blank/long context behavior, the four bounded sampling fields, full-range `seed`, explicit-zero values, and unknown-property rejection in `backend/src/test/java/com/example/chat/dto/ClassificationRequestTest.java`
- [X] T021 [P] [US2] Add failing schema and direct Jakarta `Validator` tests proving all record components are required, strings are nonblank, absent/null `confianza` is rejected rather than becoming zero, and confidence accepts only integers from 0 through 100 in `backend/src/test/java/com/example/chat/dto/ClasificacionTest.java`
- [X] T022 [P] [US2] Add failing service tests that capture the fixed `extractor` template, map external `text` to internal `question`, preserve request-scoped sampling options, apply `useProviderStructuredOutput()` and `validateSchema()`, explicitly validate the converted object, reject invalid typed results, and retain final response metadata in `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`
- [X] T023 [P] [US2] Add failing MockMvc tests for valid classification, unknown or forbidden prompt-control properties and invalid values rejected before the service, schema/conversion/Bean Validation failure, provider failure, generated nonblank `requestId` on every error, and complete metric nullability in `backend/src/test/java/com/example/chat/ClassificationControllerTest.java`

### Implementation for User Story 2

- [X] T024 [P] [US2] Create the flat validated classification input with `text`, null-preserving `rol`, `dominio`, and `idioma`, strict unknown-property rejection, and inherited sampling fields in `backend/src/main/java/com/example/chat/dto/ClassificationRequest.java`
- [X] T025 [P] [US2] Create `Clasificacion` as a Java record whose three components use `@JsonProperty(required = true)`, whose strings use `@NotBlank`, and whose wrapper-typed `Integer confianza` uses `@NotNull`, `@Min(0)`, and `@Max(100)` in `backend/src/main/java/com/example/chat/dto/Clasificacion.java`
- [X] T026 [P] [US2] Create the classification response envelope with nullable error content and all feature 003 metrics in `backend/src/main/java/com/example/chat/dto/ClassificationResponse.java`
- [X] T027 [US2] Extend the model boundary with a typed classification result that retains provider metadata in `backend/src/main/java/com/example/chat/ModelService.java`
- [X] T028 [US2] Implement classification with the fixed registered `extractor` resource, map `text` to internal `question`, apply conditional `OllamaChatOptions`, call `responseEntity(Clasificacion.class, spec -> spec.useProviderStructuredOutput().validateSchema())`, and explicitly invoke Jakarta `Validator` on the converted entity in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`
- [X] T029 [US2] Implement validated `POST /api/v1/classifications`, generated nonblank request IDs, elapsed time, tokens-per-second calculation, typed success mapping, and no raw-content logging in `backend/src/main/java/com/example/chat/ClassificationController.java`
- [X] T030 [US2] Map structured-output, conversion, post-conversion constraint, and provider failures for both endpoints to the established safe error shape with a generated nonblank request ID and without leaking model content in `backend/src/main/java/com/example/chat/exception/GlobalExceptionHandler.java`
- [X] T031 [P] [US2] Add typed classification request/response interfaces and the `/api/v1/classifications` call in `frontend/src/app/services/chat.service.ts`
- [X] T032 [US2] Add the chat/classification operation selector and render `categoria`, `confianza`, `justificacion`, and metrics in `frontend/src/app/chat/chat.component.ts`, `frontend/src/app/chat/chat.component.html`, and `frontend/src/app/chat/chat.component.css`
- [X] T033 [US2] Run and make the DTO, schema, model-service, and MockMvc classification tests pass with `gradle -p backend test --tests '*Classification*' --tests '*ClasificacionTest' --tests '*SpringAiModelServiceTest' --no-daemon` using `backend/build.gradle`

**Checkpoint**: User Story 2 returns a validated typed classification and metrics through its dedicated endpoint and frontend mode.

---

## Phase 5: User Story 3 — Reject Unsafe Template Control Without Regressions (Priority: P3)

**Goal**: Reject every non-registered identifier before provider invocation, sanitize logs and errors, and prove that features 003 and 004 and legacy requests still work.

**Independent Test**: Submit blank, unknown, differently cased, whitespace-padded, path-like, and prompt-like identifiers and verify HTTP 400 with zero model calls; then run legacy, sampling-boundary, explicit-zero, metrics, sequential, and concurrent regression tests.

### Tests for User Story 3

- [X] T034 [P] [US3] Add failing MockMvc tests for existing field-validation failures; blank, unknown, case-changed, whitespace-padded, path-like, and prompt-like `templateId` values; and separate `systemPrompt`, `promptPath`, `resource`, template-body, and other unknown properties; assert HTTP 400, generated nonblank `requestId`, safe field errors, and zero model invocations in `backend/src/test/java/com/example/chat/ChatControllerTest.java`
- [X] T035 [P] [US3] Extend model-service regression tests for omitted options, explicit `temperature=0.0` and `topK=0`, all sampling boundaries, nullable metadata, and sequential/concurrent template-context isolation in `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`
- [X] T036 [P] [US3] Add log-capture tests proving questions, answers, rendered prompts, classification text, and rejected identifier values are absent from operational logs in `backend/src/test/java/com/example/chat/filter/LogRedactionFilterTest.java` and `backend/src/test/java/com/example/chat/exception/GlobalExceptionHandlerTest.java`

### Implementation for User Story 3

- [X] T037 [US3] Add safe unknown-template and unknown-property handling that returns HTTP 400 with a generated nonblank request ID before any provider call and reuses the existing validation envelope in `backend/src/main/java/com/example/chat/prompt/PromptTemplateRegistry.java` and `backend/src/main/java/com/example/chat/exception/GlobalExceptionHandler.java`
- [X] T038 [US3] Remove or sanitize any logging of raw request, response, rendered prompt, classification, exception payload, or rejected value while retaining operation, status, duration, model, and numeric metrics in `backend/src/main/java/com/example/chat/ChatController.java`, `backend/src/main/java/com/example/chat/ClassificationController.java`, `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`, `backend/src/main/java/com/example/chat/exception/GlobalExceptionHandler.java`, and `backend/src/main/java/com/example/chat/filter/LogRedactionFilter.java`
- [X] T039 [P] [US3] Add frontend regression tests for legacy chat submission, registered-only selection, sampling payloads, and absence of free-form prompt/path controls in `frontend/src/app/chat/chat.component.spec.ts` and `frontend/src/app/services/chat.service.spec.ts`
- [X] T040 [US3] Run the complete backend regression suite and resolve only feature-related failures with `gradle -p backend test --no-daemon` using `backend/build.gradle`

**Checkpoint**: All user stories are functional, unsafe template control is rejected without model work, and features 003/004 remain compatible.

---

## Phase 6: Polish, Contract, and End-to-End Verification

**Purpose**: Synchronize public documentation and prove both core flows in the reproducible local environment.

- [X] T041 [P] Synchronize implemented chat fields, strict unknown-property behavior, classification schemas, examples, status codes, generated nonblank error request IDs, nullability, and confidence range with `specs/005-prompt-templates-structured-output/contracts/chat-api.yaml`
- [X] T042 [P] Update implementation-accurate build, run, validation, chat, and frontend instructions plus environment variables, practical minimum hardware, CPU fallback/GPU optionality, and `gemma3:4b` size, provenance, license/terms links without experiment instructions in `specs/005-prompt-templates-structured-output/quickstart.md` and `frontend/README.md`
- [X] T043 Extend the API smoke test with default and selected templates, unknown-template HTTP 400, structured classification assertions, sampling values, and feature 003 metrics in `scripts/smoke_test.sh`
- [X] T044 Extend the real browser acceptance path for a template-selected chat and a structured classification, including request payload and metric assertions, in `tests/integration/frontend-chat-flow.spec.mjs` and `frontend/playwright.config.mjs`
- [X] T045 Update the CI gate to run `npm ci`, the Angular build, headless Angular unit tests, backend tests, the updated smoke test, and Playwright without adding experiment execution in `.github/workflows/smoke-test.yml`
- [X] T046 Execute the documented backend, frontend, smoke, and Playwright verification commands from `specs/005-prompt-templates-structured-output/quickstart.md`; verify its environment, hardware, GPU/CPU, model provenance/license, and error-request-ID documentation is complete; record only pass/fail implementation evidence there and do not collect experiment repetitions, comparative metrics, tables, charts, costs, or conclusions

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 — Setup**: Starts immediately.
- **Phase 2 — Foundation**: Depends on T001; T002 prepares shared backend requests and T003–T008 activate and verify Angular. It blocks both user stories.
- **Phase 3 — US1**: Depends on T002–T008; delivers the MVP prompt-template path through the deployed Angular UI.
- **Phase 4 — US2**: Depends on T002–T008 and reuses the closed registry and renderer completed by T015–T016.
- **Phase 5 — US3**: Depends on the US1 and US2 flows it secures and regression-tests.
- **Phase 6 — Polish**: Depends on all selected user stories being implemented.

### User Story Dependencies

```text
T001 Baseline review
  └── T002–T008 Shared sampling model and deployed Angular runtime
       └── US1 Registered chat templates (MVP)
            ├── US2 Structured classification
            └── US3 Safe rejection and regression
                 └── Contract, smoke, Playwright, and final verification
```

- **US1 (P1)**: First independently demonstrable increment and source of the shared template registry.
- **US2 (P2)**: Reuses the registry/renderer but has an independent endpoint, DTOs, tests, and response contract.
- **US3 (P3)**: Validates rejection and compatibility across the two completed flows.

### Within Each User Story

- Write the listed tests first and verify they fail for the missing behavior.
- Create DTOs/resources before service integration.
- Complete model-boundary behavior before controller/frontend integration.
- Validate the story checkpoint before moving to the next priority.
- Never log or persist raw user/model content during debugging.

### Foundational Angular Work

```text
T003 Angular configuration → T004 baseline tests
T003 → T005 Angular bootstrap
T004 + T005 → T006 port existing feature 003/004 UI
T006 → T007 Docker build/serve path → T008 build and unit verification
```

## Parallel Opportunities

### User Story 1

```text
In parallel: T009 ChatRequest tests, T010 registry tests, T011 service tests, T012 controller tests
In parallel after tests: T013 ChatRequest fields, T014 prompt resources, T018 frontend request types
Then sequentially: T015 registry → T016 service → T017 controller → T019 UI
```

### User Story 2

```text
In parallel: T020 request tests, T021 schema tests, T022 service tests, T023 controller tests
In parallel after tests: T024 request DTO, T025 Clasificacion, T026 response DTO, T031 frontend service
Then sequentially: T027 model contract → T028 service → T029 controller → T030 errors → T032 UI → T033 verification
```

### User Story 3 and Polish

```text
In parallel: T034 rejection tests, T035 regression tests, T036 logging tests, T039 frontend tests
After all stories: T041 OpenAPI and T042 documentation can run in parallel
Then: T043 smoke → T044 Playwright → T045 CI review → T046 complete verification
```

## Implementation Strategy

### MVP First

1. Complete T001–T008.
2. Complete T009–T019 for User Story 1.
3. Stop and independently verify registered selection, the `conciso` default, controlled variables, literal JSON rendering, sampling, and metrics.
4. Demo this chat-template increment before adding classification if a smaller delivery is needed.

### Incremental Delivery

1. **Foundation + US1**: Editable registered templates and controlled chat selection.
2. **Add US2**: Dedicated typed classification with provider schema, validation, and metrics.
3. **Add US3**: Harden identifier rejection, log privacy, concurrency isolation, and feature 003/004 regression.
4. **Polish**: Synchronize OpenAPI/Quickstart and run smoke plus browser acceptance.

## Notes

- `[P]` means the task changes independent files and can proceed concurrently after its prerequisites.
- All template lookups are exact and server-controlled; never concatenate client input into a path.
- `responseEntity(...)` is intentional because plain `entity(...)` does not retain the `ChatResponse` metadata needed for feature 003.
- The final verification checks implementation behavior only. Experiment execution and analysis remain explicitly out of scope.
