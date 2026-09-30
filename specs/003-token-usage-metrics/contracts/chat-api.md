# Chat API Contract: Token Usage Metrics

## Endpoint

`POST /api/v1/chat`

The endpoint path, method, request shape, and existing response fields remain compatible with current clients.

Before implementation, the existing response contains only `requestId`, `answer`, `elapsedMs`, and `status`; this feature adds the six metric fields below without changing the request.

## Request

`Content-Type: application/json`

```json
{
  "sessionId": "demo",
  "question": "Pregunta de ejemplo"
}
```

- `sessionId`: optional opaque correlation value; it must not contain personal information.
- `question`: required non-blank user question.

## Successful response

```json
{
  "requestId": "00000000-0000-0000-0000-000000000000",
  "answer": "Respuesta de ejemplo",
  "elapsedMs": 1234,
  "status": "ok",
  "promptTokens": 12,
  "completionTokens": 8,
  "totalTokens": 20,
  "model": "llama3",
  "finishReason": "stop",
  "tokensPerSecond": 6.48
}
```

Existing fields:

- `requestId`: server-generated correlation identifier.
- `answer`: returned model text.
- `elapsedMs`: elapsed endpoint duration in integer milliseconds.
- `status`: `ok` for a successful model answer.

Added fields:

- `promptTokens`: provider-reported input token count, or `null`.
- `completionTokens`: provider-reported output token count, or `null`.
- `totalTokens`: provider-reported total, or `null`; compare with the component sum without replacing it.
- `model`: provider-reported model identifier, or `null`.
- `finishReason`: result metadata stop reason, or `null`.
- `tokensPerSecond`: `completionTokens / (elapsedMs / 1000)`, rounded to two decimal places, when valid; otherwise `null`.

All added fields are present in the JSON shape. `null` means the value was unavailable; zero is not used as a placeholder for missing data.

## Error response

Validation, model failure, and model-unavailable responses preserve the applicable existing status/correlation behavior and include the complete response shape. Metrics unavailable before a model response exists are `null`.

```json
{
  "requestId": "00000000-0000-0000-0000-000000000000",
  "answer": "",
  "elapsedMs": 0,
  "status": "error",
  "promptTokens": null,
  "completionTokens": null,
  "totalTokens": null,
  "model": null,
  "finishReason": null,
  "tokensPerSecond": null
}
```

The existing HTTP status semantics remain: client validation errors are 4xx, model gateway failures are 502, and unexpected runtime failures are 500.

## Compatibility and privacy

- Clients that read only `requestId`, `answer`, `elapsedMs`, and `status` continue to work because the request and existing fields are unchanged.
- No new endpoint or request field is introduced.
- Application logs may include request/session correlation, status, elapsed time, model, and aggregate counts, but must not include the question, answer, tokenized text, or personal information.
