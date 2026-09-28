# Pruebachat

This demo shows an Angular frontend and Spring Boot backend integrated with a local model runtime.

## Ollama (local model)
The project uses Ollama as the local model runtime. To start Ollama and related services:

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

To download and run a model inside the running ollama container (example using `llama3`):

```bash
docker exec -it pruebachat_ollama ollama run llama3
```

Note: model names and availability depend on Ollama's model repository and licensing. Ensure you
have the right to download and use the selected model.

## Configuration
The backend reads Ollama settings from `backend/src/main/resources/application.properties`:

```
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.default-model=llama3
```

Adjust the model and base URL as needed.
