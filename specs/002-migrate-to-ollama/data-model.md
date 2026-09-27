# Data Model: migrate-to-ollama

This migration does not introduce new persistent entities. It updates the runtime used for
model inference. Existing entities (UserQuestion, ModelResponse, SessionContext) remain unchanged.

Validation rules remain as previously defined in specs/001-spring-ai-demo/data-model.md.
