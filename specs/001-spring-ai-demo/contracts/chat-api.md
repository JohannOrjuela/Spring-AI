# Contract: Chat API

## Endpoint
POST /api/v1/chat

## Request (application/json)

```json
{
  "sessionId": "string",
  "question": "string"
}
```

- sessionId: optional string to correlate session (opaque, no PII)
- question: required string with user question

## Response (application/json)

```json
{
  "requestId": "00000000-0000-0000-0000-000000000000",
  "answer": "This is an example answer from the demo model.",
  "elapsedMs": 123,
  "status": "ok"
}
```

- requestId: server-generated UUID for tracing
- answer: model response text
- elapsedMs: integer milliseconds taken to produce response
- status: "ok" or "error"

## Error Response

```json
{
  "requestId": "string",
  "error": "string",
  "status": "error"
}
```

## Notes
- API MUST validate input and return 4xx on client errors, 5xx on server errors.
- Contract is versioned via the path (/api/v1/); bump to v2 for breaking changes.
