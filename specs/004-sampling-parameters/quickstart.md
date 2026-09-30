# Quickstart: Per-Request Sampling Parameters

## Prerequisites

- Docker and Docker Compose
- Java 21 for local Gradle tests
- Node.js 24 and npm for the browser acceptance test
- `curl` and `jq` for endpoint verification
- Enough local resources to run the repository's configured `gemma3:4b` Ollama model; GPU acceleration is optional

## Automated verification

From the repository root:

```powershell
gradle -p backend test --no-daemon
```

Coverage must include nullable wrappers, inclusive boundaries, explicit zeros, omitted/null fields, field-level HTTP 400 responses, proof that invalid input does not call the model, per-request option isolation, sanitized logs, and regression coverage for feature 003 metrics.

## Run the local stack

```powershell
docker compose -f docker/docker-compose.yml up -d --build
```

The first start downloads the configured model and can take several minutes. No sampling property needs to be edited and no service needs to be restarted between requests.

## Verify a request with options

```powershell
$body = @{
  question = 'Escribe una frase breve.'
  sessionId = 'sampling-demo'
  temperature = 0.7
  topP = 0.9
  topK = 40
  numPredict = 50
  seed = 7
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/chat' -ContentType 'application/json' -Body $body
```

Expected: HTTP 200 with the unchanged feature 003 fields, including `completionTokens`, `elapsedMs`, `tokensPerSecond`, and `finishReason`.

Internally, the service creates one fresh `OllamaChatOptions.Builder` per request and passes it to Spring AI only when at least one optional value is non-null.

## Verify default preservation and explicit zero

Omit every optional field:

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/chat' -ContentType 'application/json' -Body '{"question":"Respuesta breve"}'
```

Send valid explicit zero values:

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/chat' -ContentType 'application/json' -Body '{"question":"Respuesta breve","temperature":0.0,"topP":0.0,"topK":0,"seed":0}'
```

Both requests must be accepted. The first preserves every configured default; the second forwards the zero values intentionally.

## Verify validation before model invocation

```powershell
try {
  Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/chat' -ContentType 'application/json' -Body '{"question":"No debe llegar al modelo","temperature":17,"numPredict":4096}'
} catch {
  $_.ErrorDetails.Message
}
```

Expected: HTTP 400, `status: "error"`, a general message in `answer`, null feature 003 metrics, and `errors` entries for `temperature` and `numPredict`. Logs must not contain the question or rejected numeric values.

## Live smoke verification

```powershell
bash scripts/smoke_test.sh
```

The smoke flow must retain its feature 003 assertions and include at least one valid request with sampling options plus one invalid request that returns HTTP 400.

## Frontend-to-model regression

With the Docker Compose stack running:

```powershell
Push-Location frontend
npm ci
npx playwright install chromium
npm run test:e2e
Pop-Location
```

`npm ci` installs the locked test dependency and `playwright install chromium` installs the required browser on a clean machine. Open the optional sampling panel in the frontend to set any subset of `temperature`, `topP`, `topK`, `numPredict`, and `seed`; blank fields preserve Ollama defaults. The browser test submits all five values through the form and verifies both the outgoing JSON and the returned answer with feature 003 metrics.

## References

- API contract: [contracts/chat-api.yaml](contracts/chat-api.yaml)
- Field model and null rules: [data-model.md](data-model.md)
- Design decisions: [research.md](research.md)
