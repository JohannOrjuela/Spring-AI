# Data Model: Token Usage Metrics

## ChatResponse

Existing public response DTO returned by `POST /api/v1/chat`.

| Field | Type | Required in JSON | Meaning |
|---|---|---:|---|
| `requestId` | string or null | Yes | Server-generated correlation identifier when available. |
| `answer` | string | Yes | Model answer, or the existing empty error value when no answer exists. |
| `elapsedMs` | integer | Yes | Monotonic elapsed duration converted to milliseconds for the request's model call. |
| `status` | string | Yes | Existing success/error status. |
| `promptTokens` | integer or null | Yes | Provider-reported input token count. |
| `completionTokens` | integer or null | Yes | Provider-reported output token count. |
| `totalTokens` | integer or null | Yes | Provider-reported total token count; not locally normalized. |
| `model` | string or null | Yes | Provider/model identifier from response metadata. |
| `finishReason` | string or null | Yes | Completion stop reason from result metadata. |
| `tokensPerSecond` | number or null | Yes | `completionTokens / (elapsedMs / 1000)`, rounded to two decimal places, when both inputs support the calculation. |

### Validation and invariants

- Existing fields and meanings remain unchanged.
- All six new fields are emitted even when unavailable; unavailable values are JSON `null`.
- `totalTokens` is copied from provider metadata. It is compared with `promptTokens + completionTokens` in tests/documentation but is not overwritten.
- `tokensPerSecond` is rounded to two decimal places and is `null` when completion tokens are unavailable, elapsed time is zero/non-positive, or the calculation cannot be performed safely.
- The value uses the existing `elapsedMs` response field as the denominator in seconds.
- No field contains the submitted question, generated answer beyond the existing `answer` contract, or personal information except the intended answer content returned to the caller.

## Internal model result

The service boundary must return an enriched internal result rather than only `String`, containing:

- answer text;
- provider usage values;
- provider model identifier;
- finish reason;
- availability represented as nullable values.

This is an internal mapping entity and is not a new HTTP resource.

## Error response state

When model metadata is unavailable, the existing error status/correlation behavior remains and all metric fields are present with `null`. No synthetic token count, model, finish reason, or throughput is generated.

