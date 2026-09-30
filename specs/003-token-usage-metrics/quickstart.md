# Quickstart: Token Usage Metrics

## Prerequisites

- Docker and Docker Compose
- Java 21 if running Gradle tests locally
- `jq` for validating the smoke-test JSON response
- Ollama service and the configured `llama3` model available through the repository Compose setup

## Deterministic backend validation

From the repository root:

```bash
gradle -p backend test --no-daemon
```

Expected coverage includes:

- Spring AI response metadata mapped into the enriched internal result;
- `ChatResponse` JSON fields and Jackson empty-constructor compatibility;
- `tokensPerSecond` arithmetic and null handling;
- MockMvc success, validation, provider failure, and runtime failure responses;
- compatibility for clients that read only the original four response fields;
- assertions that question, answer, tokenized text, and PII do not appear in application logs.

## End-to-end validation with Ollama

Start the local services:

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

Wait until the model pull has completed, then run:

```bash
bash scripts/smoke_test.sh
```

The response must include a non-empty `answer`, the preserved fields, and the six token metrics. For a successful live response, `promptTokens`, `completionTokens`, `totalTokens`, `model`, and `finishReason` should be populated by the real Spring AI/Ollama response. Compare `totalTokens` with the component sum without replacing the provider value.

## Expected privacy behavior

Inspect backend logs during tests and the smoke flow. They may contain non-sensitive correlation and aggregate metrics, but must not contain either fixed prompt, generated answer, tokenized text, or personal information.

## References

- Response contract: [contracts/chat-api.md](contracts/chat-api.md)
- Data model and null rules: [data-model.md](data-model.md)
- Research decisions: [research.md](research.md)
