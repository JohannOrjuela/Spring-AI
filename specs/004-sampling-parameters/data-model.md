# Data Model: Per-Request Sampling Parameters

## ChatRequest

Existing request DTO for `POST /api/v1/chat`, extended with five optional properties.

| Field | Java type | Required | Validation and semantics |
|---|---|---:|---|
| `sessionId` | `String` | No | Existing opaque session value; unchanged. |
| `question` | `String` | Yes | Existing non-blank validation; unchanged. |
| `temperature` | `Double` | No | Inclusive 0.0–2.0. Null/omitted preserves the configured default; 0.0 is explicit. |
| `topP` | `Double` | No | Inclusive 0.0–1.0. Null/omitted preserves the configured default; 0.0 is explicit. |
| `topK` | `Integer` | No | Inclusive 0–200. Null/omitted preserves the configured default; 0 is explicit. |
| `numPredict` | `Integer` | No | Inclusive 1–2048. Null/omitted preserves the configured default. |
| `seed` | `Integer` | No | Any Java integer, including zero and negative values. Null/omitted preserves provider behavior. |

### Validation invariants

- Any out-of-range bounded field invalidates the complete request.
- The controller must not invoke `ModelService` when validation fails.
- Wrapper types are mandatory so Jackson does not manufacture zero for omitted fields.
- Values exactly equal to a minimum or maximum are valid.
- No request value mutates shared configuration or later requests.

## Request-scoped Ollama options

Transient provider configuration derived from one validated `ChatRequest`.

- Created inside `SpringAiModelService` for the current call only.
- Includes only non-null request fields.
- Is not cached, persisted, or assigned to the shared `ChatClient`.
- Is not created/applied when all sampling fields are null.

## ChatResponse

The feature 003 response remains authoritative. The fields `requestId`, `answer`, `elapsedMs`, `status`, `promptTokens`, `completionTokens`, `totalTokens`, `model`, `finishReason`, and `tokensPerSecond` retain their current meanings and nullability.

For validation failures:

- `requestId` is generated as today.
- `answer` carries a stable general validation message.
- `elapsedMs` is `0` because the model is not called.
- `status` is `error`.
- all six feature 003 metric/model fields are present and null.
- additive `errors` contains one entry per invalid field.

For successful or provider-failure responses, `errors` is absent or null and existing behavior remains unchanged.

## ValidationError

Value embedded in the `errors` collection.

| Field | Type | Meaning |
|---|---|---|
| `field` | string | Public JSON property that failed validation. |
| `message` | string | Stable, comprehensible range/required-field message. |

Rejected values are deliberately excluded from this entity and from logs.
