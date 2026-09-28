# Research: migrate-to-ollama

## Decisions

- Use Ollama local runtime (image: ollama/ollama:latest) to host models locally for the demo.
- Default model: `llama3` (fallback `mistral` if unavailable).
- Dependency: add `spring-ai-ollama-spring-boot-starter` (placeholder coordinates used; confirm vendor/version).

## Model provenance & licensing

- Action: verify license for `llama3` or chosen model before distribution; record provenance in README.

## Integration surface

- Ollama exposes an HTTP API on port 11434. Integration will call the configured base URL with a
  simple JSON request containing the model and prompt. The LocalModelClient maps to the base URL and posts
  payload `{ "model": "llama3", "prompt": "..." }` and expects a JSON response containing `answer` or `text`.

## Quickstart validation

- Quickstart will start Ollama via docker-compose and provide instructions to pull/run a model inside the container.

## Conclusion
- Proceed with implementation; confirm dependency coordinates in plan step.
