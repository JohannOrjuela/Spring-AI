# Contracts & Integration Notes

- The existing Chat API contract (POST /api/v1/chat) remains unchanged.
- Backend will call Ollama runtime at configured base URL with payload `{ "model": "llama3", "prompt": "..." }`.
- Expected Ollama response: JSON containing either `answer` or `text` properties with the generated output.

