# Research: Per-Request Sampling Parameters

## Decision: Use `OllamaChatOptions`

**Rationale**: The project is pinned to Spring AI 2.0.0. Spring AI documents `OllamaChatOptions` as the type-safe chat-model runtime option class and marks `OllamaOptions` as deprecated. Request-specific options override corresponding `spring.ai.ollama.chat.*` defaults only for that call.

**Alternatives considered**: `OllamaOptions` was rejected because it is deprecated. Editing `application.properties` was rejected because it changes global behavior and requires a restart.

**Primary reference**: [Spring AI Ollama Chat — Runtime Options](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)

## Decision: Model optional inputs with wrapper types

**Rationale**: `Double` and `Integer` preserve three required states: omitted/null, explicit zero, and an explicit non-zero value. Primitive values would deserialize omitted fields as zero and incorrectly replace configured defaults.

**Alternatives considered**: Primitive fields were rejected because omission becomes indistinguishable from valid `temperature=0.0`, `topP=0.0`, or `topK=0`. `Optional` DTO fields were rejected because nullable Jackson properties plus Bean Validation are simpler in the current codebase.

## Decision: Forward only supplied fields and omit `.options(...)` when none exist

**Rationale**: Build `OllamaChatOptions` by invoking a fresh builder setter only when the corresponding request value is non-null. If all five values are null, keep the existing prompt chain without `.options(...)`; this gives the strongest guarantee that configured defaults remain untouched. In Spring AI 2.0.0, `ChatClient.ChatClientRequestSpec.options(...)` accepts the typed `OllamaChatOptions.Builder`, so the request-scoped builder is passed through `.options(options)` before `.call().chatResponse()`.

**Alternatives considered**: Always applying an empty options instance was rejected because default-merging behavior would become an unnecessary dependency. Rebuilding an options object from global properties was rejected because it duplicates Spring AI configuration.

## Decision: Validate at the HTTP boundary before invoking the service

**Rationale**: Jakarta Bean Validation on `ChatRequest`, combined with the controller's existing `@Valid`, rejects invalid values before `ModelService` is called. Inclusive annotations map directly to the contract: `temperature` 0.0–2.0, `topP` 0.0–1.0, `topK` 0–200, and `numPredict` 1–2048. `seed` has no range annotation and accepts the full Java `Integer` domain.

**Alternatives considered**: Provider-side validation was rejected because invalid input would reach the model boundary and yield less stable errors. Manual controller comparisons were rejected because they duplicate declarative validation.

## Decision: Preserve the existing response and add field errors only for validation failure

**Rationale**: The current error shape uses `requestId`, `answer`, `elapsedMs`, `status`, and nullable feature 003 metrics. For HTTP 400, `answer` remains the general error message and an additive `errors` collection contains `{field, message}` entries. Existing metric fields remain present and null. This avoids a second error DTO with a divergent metric shape.

**Alternatives considered**: Replacing `ChatResponse` with a separate validation DTO was rejected because feature 003 requires a stable error shape. Returning raw `BindingResult` was rejected because it is framework-specific and can expose rejected values.

## Decision: Pass `ChatRequest` through the existing service boundary

**Rationale**: Changing `ModelService.generateResponse` to accept the validated request keeps the five related nullable values together and lets `SpringAiModelService` build provider options without a long parameter list. In this small demo, a second mapping object would add indirection without a second consumer.

**Alternatives considered**: Five additional method parameters were rejected as brittle. A dedicated domain command was rejected for this feature's limited scope, though it can be introduced later if another transport or service needs the same data.

## Decision: Test isolation, default preservation, and metadata regression separately

**Rationale**: MockMvc tests prove validation, HTTP 400, field details, and zero/null behavior. Service tests capture `OllamaChatOptions` to prove only supplied values are set and that requests do not share mutable state. Existing feature 003 assertions remain to prove metadata mapping and nullable metrics have not changed. A live smoke request confirms the complete endpoint-to-Ollama path.

**Alternatives considered**: Live-only coverage was rejected because option omission and invalid-before-model behavior need deterministic assertions. Mock-only coverage was rejected because it would not verify the real integration path.

## Decision: Sanitize validation logging

**Rationale**: `BindingResult.getAllErrors().toString()` can include rejected user values. Validation logs will contain only the request correlation identifier, invalid field names, and validation codes; they will not include question text, answers, or rejected values.

**Alternatives considered**: Logging the complete binding errors was rejected by the constitution's data-minimization rule. Suppressing validation logs entirely was rejected because safe diagnostic context is useful.
