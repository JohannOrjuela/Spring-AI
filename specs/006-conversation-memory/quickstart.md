# Quickstart: Conversation Memory

## Purpose

This guide validates feature `006-conversation-memory` locally after implementation. It covers deterministic tests, stateful and stateless chat behavior, isolation, restart scope, and regressions for features 003–005. Experiment E is a separate explicit 20-turn command and is never part of the default build, smoke test, or CI.

## Prerequisites

- Docker and Docker Compose
- Java 21 and Gradle 8.14.5, or Docker for the backend-test fallback
- Node.js 24 and npm for the Angular build, unit tests, and Playwright
- Bash, curl, and jq for smoke checks and experiment E
- PowerShell examples assume execution from the repository root

## Local hardware and model baseline

- 64-bit host capable of running Docker Desktop or Docker Engine
- 8 GB system RAM minimum; additional RAM improves CPU inference
- At least 12 GB free disk for the model, images, build layers, and dependencies
- CPU execution is supported but may be slow; GPU acceleration is optional and not assumed by Compose
- Model: `gemma3:4b`, developed by Google and distributed locally through Ollama
- Review the [Ollama model entry](https://ollama.com/library/gemma3:4b), [Gemma 3 model card](https://ai.google.dev/gemma/docs/core/model_card_3), and [Gemma terms](https://ai.google.dev/gemma/terms) before redistribution

Compose publishes Ollama on `11434`, the backend on `8080`, and the Angular frontend on `4200`. It supplies `OLLAMA_HOST=http://ollama:11434` to the pull job and `SPRING_AI_OLLAMA_BASE_URL=http://ollama:11434` to the backend. This local setup requires no API key; future credentials must use environment variables or a secrets mechanism, never source-controlled files or prompts.

## 1. Run deterministic tests

```powershell
gradle -p backend test --no-daemon

cd frontend
npm ci
npm run build
npm test -- --watch=false
cd ..
```

When Gradle is unavailable locally:

```powershell
docker run --rm -v "${PWD}:/workspace" -v spring-ai-006-gradle-cache:/home/gradle/.gradle -w /workspace gradle:8.14-jdk21 gradle -p backend test --no-daemon
```

Expected backend coverage includes exact session isolation, stateless requests, blank rejection, prompt-window boundaries at 19/20/21 non-system messages, system preservation, success-only commit, same-session serialization, independent sessions, and feature 003–005 regressions.

## 2. Start the local application

```powershell
docker compose -f docker/docker-compose.yml up -d --build
```

Wait until `http://localhost:8080/health/llm` returns `ok: true`. The first startup may download the model. Open `http://localhost:4200`; the existing browser chat generates one identifier and reuses it for later chat turns, while classification remains stateless.

## 3. Verify same-session continuity

```powershell
$session = "memory-demo-$([guid]::NewGuid())"

$first = @{
  sessionId = $session
  question = "Recuerda este dato sintetico: mi codigo es ALFA-006. Responde listo."
  templateId = "conciso"
  temperature = 0.0
  seed = 7
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/chat" -ContentType "application/json" -Body $first

$followUp = @{
  sessionId = $session
  question = "Cual es el codigo sintetico que indique?"
  templateId = "tutor"
  rol = "verificador"
  dominio = "memoria"
  idioma = "espanol"
  temperature = 0.0
  seed = 7
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/chat" -ContentType "application/json" -Body $followUp
```

Expected: both calls return the unchanged successful response envelope and metrics. The second request uses the retained first turn, but its own `tutor` template, context, and sampling values. Provider `promptTokens` reflect the actual prompt including retained history.

## 4. Verify isolation and exact identifiers

Send a different synthetic fact under a different identifier, then ask each identifier about its own fact. Also treat `Case`, `case`, and ` Case ` as three distinct nonblank identifiers. Do not include real user identifiers or personal data.

Expected: no answer receives messages or request-scoped settings from another exact identifier. Application logs must not contain identifiers, prompts, retained messages, or answers.

## 5. Verify stateless and invalid identifier behavior

Omit `sessionId` twice while asking a follow-up. Each call must be independent. An explicit `null` behaves the same way.

To verify blank rejection:

```powershell
$invalid = @{ sessionId = "   "; question = "Hola" } | ConvertTo-Json
try {
  Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/chat" -ContentType "application/json" -Body $invalid
} catch {
  $_.ErrorDetails.Message
}
```

Expected: HTTP 400 before provider work, a generated nonblank `requestId`, `answer="Request validation failed"`, a `sessionId` field error, and the complete nullable metric shape.

## 6. Verify process-local restart scope

Create a stateful synthetic turn, restart only the backend, wait for health, and reuse the same identifier:

```powershell
docker compose -f docker/docker-compose.yml restart backend
```

Expected: the service starts with no retained history. There is no database, memory volume, cross-instance sharing, history endpoint, or expiry API. Model prose is nondeterministic, so automated proof uses an isolated application-context restart test rather than phrase matching.

## 7. Run bounded smoke and browser acceptance

```powershell
bash scripts/smoke_test.sh
npm --prefix frontend exec -- playwright install chromium
npm --prefix frontend run test:e2e
```

The smoke and browser suites may cover a small same-session regression, template selection, sampling, classification, metrics, and safe errors. They must not invoke experiment E.

On Windows without Bash, curl, or jq, the smoke test can run in a disposable container after the Compose services are healthy:

```powershell
docker run --rm --network container:pruebachat_backend -v "${PWD}/scripts:/scripts:ro" -e BASE_URL=http://localhost:8080 alpine:3.22 sh -c "apk add --no-cache bash curl jq >/dev/null && bash /scripts/smoke_test.sh"
```

## 8. Run experiment E explicitly

Experiment E uses exactly 20 fixed, non-sensitive prompts and one generated session identifier. It validates transport, HTTP status, and every response envelope; it reports recent/old-context observations without treating nondeterministic model wording as a hard test.

```powershell
bash scripts/experiment_e.sh
```

To target another local backend:

```powershell
$env:BASE_URL = "http://localhost:8080"
bash scripts/experiment_e.sh
```

Expected summary: generated session identifier, `20` completed turns, zero transport/HTTP/contract failures, and labeled observations for early continuity, recent recall, and oldest-message eviction. The script fails with the turn number and nonzero exit status on any transport, HTTP, or contract violation. It writes no result file by default; redirect standard output explicitly if a workshop artifact is desired.

Recorded local validation (2026-10-01): **PASS — 20 completed turns**.

## 9. Confirm experiment exclusion

Inspect the standard commands and CI workflow:

```powershell
git grep -n "experiment_e" -- backend frontend scripts/smoke_test.sh tests .github/workflows
```

Expected: only explicit documentation or the experiment script itself references the command; Gradle, npm tests, smoke, Playwright, and `.github/workflows/smoke-test.yml` do not execute it.
