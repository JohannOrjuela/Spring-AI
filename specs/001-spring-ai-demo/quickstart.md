# Quickstart: spring-ai-demo

## Prerequisites
- Docker & docker-compose installed
- Java 25 JDK (for building backend if running locally) — optional if using
  prebuilt images

## Environment
- Create a `.env` file or export environment variables for secrets (do NOT commit):

```
MODEL_IMAGE=nvidia/model-image:latest
MODEL_API_KEY=
```

## Run locally (developer flow)
1. Build backend (optional): `./gradlew :backend:build` (or use provided image)
2. Start services: `docker-compose up --build`
3. Open frontend: http://localhost:4200

## Smoke test

Run a smoke test to validate end-to-end flow (repeatable):

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"Hello, demo","sessionId":"demo"}'
```

Expected: 200 response with JSON containing non-empty `answer` field.

## Troubleshooting
- If model container fails to start, check image name and available GPU drivers.
- For long startup times, run `docker-compose logs` and retry smoke test after
  model reports readiness.

## License & Model Provenance
Document the model image name, version, and license in README.md as recommended in research.md.
