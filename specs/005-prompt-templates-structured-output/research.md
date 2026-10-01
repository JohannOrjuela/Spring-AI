# Research: Prompt Templates and Structured Output

## Decision: Use a closed resource registry

**Rationale**: Public identifiers `conciso`, `tutor`, and `extractor` map to fixed classpath resources. Resolution never treats client input as a path or prompt body. Null selects `conciso`; blank or any non-exact identifier is rejected before model invocation. Both request DTOs reject every unknown JSON property as malformed input rather than silently ignoring it, including properties attempting to supply a separate system prompt, resource, path, or template definition. This provides one deterministic contract and editable prompt files without exposing system-prompt control.

**Alternatives considered**: Accepting prompt text was rejected as a guardrail bypass. Building a classpath path from user input was rejected because it expands the attack surface. An external prompt database was rejected as unnecessary infrastructure for the demo.

## Decision: Keep system and user templates separate

**Rationale**: The three registered system resources use controlled role, domain, and language values. A common user resource carries the internal variable `question`, preserving the distinction between trusted instructions and user content. Chat maps its external `question` directly; classification maps its external `text` to that same internal variable. At the request-contract level all three registered behaviors therefore accept the same four variables, but user content is not promoted into the system role.

**Alternatives considered**: Inserting the question into the system template was rejected because it weakens instruction boundaries. Duplicating the same user wrapper in all three files was rejected as needless drift.

## Decision: Use one immutable renderer with `<` and `>` delimiters

**Rationale**: Spring AI 2.0's `StTemplateRenderer` is thread-safe and validates missing variables by default. A single immutable instance configured with `<` and `>` can be shared across request chains, lets JSON braces remain literal, and is applied explicitly through `templateRenderer(...)`.

**Alternatives considered**: Default `{}` delimiters conflict with literal JSON examples. Manual string replacement lacks missing-variable validation. A custom template engine adds no value for this scope.

**Primary reference**: [Spring AI Chat Client — Prompt Templates](https://docs.spring.io/spring-ai/reference/api/chatclient.html#_prompt_templates)

## Decision: Normalize only missing context, reject blank supplied context

**Rationale**: Deserialized DTO fields remain null when `rol`, `dominio`, or `idioma` is null or omitted. The request-scoped prompt builder then normalizes them to `asistente`, `general`, and `el mismo idioma de la pregunta`. Explicit blank values are malformed rather than silently rewritten. This feature adds no context-length guardrail: any nonblank context value is accepted. This makes defaults predictable and avoids a separate language-detection dependency while still expressing the required behavior to the model.

**Alternatives considered**: Requiring all values would break existing clients. Hard-coding Spanish would violate the current multilingual behavior. Adding language detection would expand scope and introduce another dependency.

## Decision: Share sampling validation through a flat base DTO

**Rationale**: `ChatRequest` and `ClassificationRequest` expose the existing five sampling fields at the top level while inheriting one validated Java definition. Both also accept the same optional role, domain, and language context; only chat accepts `templateId`, while classification always resolves `extractor`. This preserves the feature 004 JSON contract and prevents range/default rules from drifting between endpoints.

**Alternatives considered**: Duplicating five fields risks divergence. A nested sampling object would break the established request shape. Allowing classification to inherit chat-only template fields would widen its contract unnecessarily.

## Decision: Combine native output and schema validation

**Rationale**: `useProviderStructuredOutput()` sends the generated schema to Ollama, while `validateSchema()` catches residual invalid JSON/schema output and performs bounded correction attempts. The `Clasificacion` schema remains shallow and compatible with provider limitations. It is represented as a Java record; `confianza` uses `Integer` with `@NotNull`, `@Min(0)`, and `@Max(100)` so an absent value cannot collapse into the valid primitive default `0`. Required Jackson annotations ensure all properties appear in the generated schema. After conversion, the service explicitly invokes Jakarta `Validator` on `Clasificacion` so nonblank strings and confidence 0–100 are enforced even if provider/schema tooling does not apply Bean Validation annotations automatically; violations produce a safe error rather than partial success.

**Alternatives considered**: Prompt instructions alone are weaker. Native-only output does not catch provider/model edge cases. Validation-only output spends prompt space on formatting and misses provider-level enforcement.

**Primary references**: [Spring AI Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output.html), [Spring AI Ollama Structured Outputs](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html#_structured_outputs)

## Decision: Use `responseEntity(...)` to preserve final response metadata

**Rationale**: The requested `entity(...)` family converts model text into `Clasificacion` but returns no response metadata. Spring AI 2.0 provides the parallel `responseEntity(Clasificacion.class, consumer)` method, which applies the same native-output and validation switches and returns both the converted entity and its `ChatResponse`. The service can therefore map the final provider response's usage, model, and finish reason without making a second call. `elapsedMs` includes any automatic correction attempts; token metadata reflects the final response exposed by Spring AI, and no retry count is added.

**Alternatives considered**: Calling once for the entity and again for metadata would double cost and could produce inconsistent outputs. Manual JSON conversion after `chatResponse()` would bypass the requested high-level validation behavior.

## Decision: Preserve the existing error and privacy conventions

**Rationale**: Validation failures return HTTP 400 before model invocation. Provider, exhausted-structure, conversion, or post-conversion Bean Validation failures use the existing safe 5xx handling. Every error envelope receives a newly generated nonblank request ID. Classification responses expose `answer` only as the established nullable general-error message; successful content lives in `classification`. Logs contain operation, status, duration, model, and numeric metrics, never raw content, rendered prompts, rejected values, exception payloads, or classification fields.

**Alternatives considered**: A second unrelated error DTO would fragment clients. Logging rendered templates or invalid payload values conflicts with the constitution.

## Decision: Extend the current frontend and acceptance gate

**Rationale**: The runtime currently serves a single inline HTML/JavaScript page; the Angular component files are not bootstrapped, built, or included in the Docker image. Because the constitution requires the Angular UI, foundational work first creates a complete Angular 20 project and unit-test target, ports the existing health probe, session ID, sampling controls, metrics display, timeout, and safe text rendering, and changes Docker to serve the Angular build output. The feature then adds the registered-template selector, optional context inputs, and chat/classification operation selector. Smoke and Playwright checks cover a template-selected chat, unknown-template rejection, and structured classification through real Ollama.

**Alternatives considered**: Continuing to edit the inactive `src/app` files while Docker served `src/index.html` was rejected because feature changes would never reach users. Keeping both implementations was rejected as drift. Retargeting all work to the inline page was rejected because it violates the constitution's Angular requirement. API-only verification would miss the required frontend-to-model path, and an experiment dashboard would add unnecessary scope.

**Primary references**: [Angular version compatibility](https://angular.dev/reference/versions) confirms Angular 20.2/20.3 supports Node.js 24; [Angular npm dependencies](https://angular.dev/reference/configs/npm-packages) identifies the CLI build package used for build and test targets.
