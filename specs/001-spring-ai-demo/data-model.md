# Data Model: spring-ai-demo

## Entities

### UserQuestion
- id: string (UUID)
- text: string (user-provided question)
- created_at: ISO-8601 timestamp
- metadata: object (non-sensitive, e.g., client_version)

### ModelResponse
- id: string (UUID)
- question_id: string (UserQuestion.id)
- text: string (model-generated response)
- created_at: ISO-8601 timestamp
- confidence: optional float (0.0-1.0) if provided by model

### SessionContext
- session_id: string (opaque, non-sensitive)
- last_interaction_at: ISO-8601 timestamp

## Relationships
- One UserQuestion → One ModelResponse (for demo: single-turn)

## Validation rules
- UserQuestion.text MUST be non-empty and <= 4000 characters for demo purposes.
- ModelResponse.text MUST be non-empty when returned successfully.

## Notes
- Keep persisted data minimal; do not store user PII. SessionContext must avoid
  storing any personal identifiers.
