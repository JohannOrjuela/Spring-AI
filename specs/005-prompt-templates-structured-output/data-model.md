# Data Model: Prompt Templates and Structured Output

## SamplingParameters

Validated base request data reused by chat and classification. Properties remain flat in JSON.

| Field | Java type | Required | Validation and semantics |
|---|---|---:|---|
| `temperature` | `Double` | No | Inclusive 0.0–2.0; null/omitted preserves Ollama default; 0.0 is explicit. |
| `topP` | `Double` | No | Inclusive 0.0–1.0; null/omitted preserves Ollama default; 0.0 is explicit. |
| `topK` | `Integer` | No | Inclusive 0–200; null/omitted preserves Ollama default; 0 is explicit. |
| `numPredict` | `Integer` | No | Inclusive 1–2048; null/omitted preserves Ollama default. |
| `seed` | `Integer` | No | Full Java integer range; null/omitted preserves provider behavior. |

## ChatRequest

Extends `SamplingParameters` while preserving its flat JSON shape.

| Field | Java type | Required | Validation and semantics |
|---|---|---:|---|
| `sessionId` | `String` | No | Existing opaque value; unchanged. |
| `question` | `String` | Yes | Must be nonblank. |
| `templateId` | `String` | No | Remains null in the DTO when null/omitted; request processing resolves it to `conciso`. Exact `conciso`, `tutor`, or `extractor` are valid; blank and every other value are invalid. |
| `rol` | `String` | No | Remains null in the DTO when null/omitted; prompt construction resolves it to `asistente`. An explicitly blank value is invalid; no maximum length is introduced. |
| `dominio` | `String` | No | Remains null in the DTO when null/omitted; prompt construction resolves it to `general`. An explicitly blank value is invalid; no maximum length is introduced. |
| `idioma` | `String` | No | Remains null in the DTO when null/omitted; prompt construction resolves it to `el mismo idioma de la pregunta`. An explicitly blank value is invalid; no maximum length is introduced. |

Both request DTOs reject unknown JSON properties rather than silently ignoring them. Consequently, fields such as `systemPrompt`, `promptPath`, `resource`, or client-defined template bodies produce HTTP 400 before registry resolution or model invocation.

## ClassificationRequest

Extends `SamplingParameters` while preserving flat sampling fields. It has no `templateId`; the operation always uses the registered `extractor` template.

| Field | Java type | Required | Validation and semantics |
|---|---|---:|---|
| `text` | `String` | Yes | Text to classify; must be nonblank. |
| `rol` | `String` | No | Same default and blank rule as chat. |
| `dominio` | `String` | No | Same default and blank rule as chat. |
| `idioma` | `String` | No | Same default and blank rule as chat. |

## PromptTemplateDefinition

Immutable registry value; it is never deserialized from a client.

| Field | Type | Meaning |
|---|---|---|
| `id` | string | Exact public identifier. |
| `systemResource` | classpath resource | Fixed server-controlled system template. |

Registry invariants:

- Exactly `conciso`, `tutor`, and `extractor` are public.
- Lookup does not concatenate, normalize, or resolve client input as a path.
- Null lookup selects `conciso`; blank and unknown lookup fail.
- Definitions and renderer are immutable and safe to share; request variables are not retained.

## TemplateContext

Request-scoped normalized values supplied to the selected system and user templates.

| Variable | Source/default | Destination |
|---|---|---|
| `rol` | Request or `asistente` | Registered system resource. |
| `dominio` | Request or `general` | Registered system resource. |
| `idioma` | Request or `el mismo idioma de la pregunta` | Registered system resource. |
| `question` | Chat `question` directly, or classification `text` mapped internally | Common user resource. |

The renderer uses `<` and `>` tokens. Literal JSON braces are not variables.

## Clasificacion

Typed structured model result.

| Field | Java type | Required | Validation |
|---|---|---:|---|
| `categoria` | `String` | Yes | JSON-schema required and nonblank. |
| `confianza` | `Integer` | Yes | `@JsonProperty(required = true)`, `@NotNull`, integer, inclusive 0–100 with `@Min(0)` and `@Max(100)`; null/absent cannot become a valid default zero. |
| `justificacion` | `String` | Yes | JSON-schema required and nonblank. |

`Clasificacion` is a Java record. All components are explicitly marked required for schema generation; string components use `@NotBlank`, and the wrapper-typed confidence uses `@NotNull`. Provider-native schema enforcement and schema validation run before conversion. After conversion, the service invokes Jakarta `Validator` explicitly and rejects any required, nonblank, or range violation before success is returned; annotations alone are not treated as proof that model output was validated.

## ClassificationResponse

Dedicated HTTP response envelope.

| Field | Type | Success | Error |
|---|---|---|---|
| `requestId` | string | Generated identifier | Generated identifier |
| `classification` | `Clasificacion` or null | Required object | null |
| `answer` | string or null | null | Stable general error message |
| `elapsedMs` | integer | Full operation duration, including automatic corrections | 0 before model; measured after model work |
| `status` | string | `ok` | `error` |
| `promptTokens` | integer or null | Final exposed provider metadata | null when unavailable |
| `completionTokens` | integer or null | Final exposed provider metadata | null when unavailable |
| `totalTokens` | integer or null | Final exposed provider metadata | null when unavailable |
| `model` | string or null | Final exposed provider metadata | null when unavailable |
| `finishReason` | string or null | Final exposed provider metadata | null when unavailable |
| `tokensPerSecond` | number or null | Derived from completion tokens and elapsed time | null when unavailable |
| `errors` | array or null | null | Safe field/message entries when applicable |

## State and failure transitions

```text
request received
  → HTTP validation
      → unknown property or invalid value: HTTP 400 with generated requestId, no model call
      → valid: normalize context and resolve fixed template
          → chat: free-form model response + metrics
          → classification: native schema + schema validation
              → convert typed object → explicit Jakarta validation
                  → valid: HTTP 200 + classification + metrics
                  → constraint violation: safe 5xx response with generated requestId
              → exhausted/failed model conversion: safe 5xx response with generated requestId
```

No request, rendered prompt, response content, or classification is persisted.
