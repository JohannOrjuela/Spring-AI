# Quickstart: migrate-to-ollama

## Prerequisites
- Docker & docker-compose installed

## Steps

1. Build and start services:

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

2. Pull/run a model inside the Ollama container (example: llama3):

```bash
docker exec -it pruebachat_ollama ollama run llama3
```

3. Verify backend is reachable and test:

```bash
./scripts/smoke_test.sh
```

Expected: smoke test returns a JSON with a non-empty `answer`.

## Notes
- The Quickstart uses the application property `spring.ai.ollama.base-url` to determine the Ollama endpoint.
- If model download is large, wait for model to finish downloading inside the container before running the smoke test.
