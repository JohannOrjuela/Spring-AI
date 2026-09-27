# Feature Specification: migrate-to-ollama

**Feature Branch**: `[###-feature-name]`

**Created**: 2026-07-27

**Status**: Draft

**Input**: User request to refactor the Spring AI project to replace the NVIDIA/OpenAI model integration
with a local Ollama instance managed via Docker Compose. Changes requested: update dependencies to
use spring-ai-ollama-spring-boot-starter, add an ollama service to docker-compose (ollama/ollama:latest),
configure persistent volume for models, set spring.ai.ollama.base-url=http://localhost:11434 and default
model to `llama3` (or `mistral`), refactor Java code to use OllamaChatModel/ChatModel, and update README
with instructions to pull/run models inside the container.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Developer migration (Priority: P1)
As a developer, I can switch the project to use a local Ollama runtime and verify the chat flow works
end-to-end locally without Nvidia-specific dependencies.

**Why this priority**: The change replaces a core runtime dependency and affects local reproducibility.

**Independent Test**: Update dependencies and docker-compose, start services with Quickstart, then run
smoke_test.sh which should return a non-empty `answer` from the backend via Ollama.

**Acceptance Scenarios**:

1. **Given** updated dependencies and docker-compose, **When** `docker compose up --build` is run,
   **Then** an `ollama` container is started and exposes port 11434 and a persistent volume holds models.
2. **Given** the app is configured with `spring.ai.ollama.base-url=http://localhost:11434` and the
   default model set to `llama3`, **When** the smoke test POSTs a question, **Then** the backend returns a
   non-empty `answer` fetched via the Ollama integration.
3. **Given** the README instructions, **When** a maintainer runs the provided `docker exec` command,
   **Then** the requested model (llama3) is downloaded inside the running container and ready for queries.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Remove Nvidia/OpenAI Spring AI starter dependencies from the project build files.
- **FR-002**: Add `spring-ai-ollama-spring-boot-starter` dependency to the backend build configuration.
- **FR-003**: Update `docker-compose.yml` to include an `ollama` service using `ollama/ollama:latest`,
  expose `11434:11434`, and mount a persistent volume `ollama_data:/root/.ollama`.
- **FR-004**: Update application properties to set `spring.ai.ollama.base-url=http://localhost:11434` and
  set the default model to `llama3` (or `mistral` as an alternative).
- **FR-005**: Refactor any Nvidia/OpenAI-specific client beans or imports to use the generic ChatModel
  abstraction or the `OllamaChatModel` implementation provided by the new starter.
- **FR-006**: Update README with instructions to pull/run a model inside the ollama container (example
  `docker exec -it <ollama-container> ollama run llama3`).

### Key Entities

- **ChatModel**: abstraction representing a chat-capable local model (provider-agnostic).
- **OllamaChatModel**: concrete implementation provided by spring-ai-ollama starter.
- **LocalModelRuntime**: the running Ollama container instance accessible at http://localhost:11434.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `mvn/gradle build` completes for the backend after dependency updates without the removed Nvidia/OpenAI starter.
- **SC-002**: `docker compose up --build` brings up an `ollama` container that listens on 11434 and a persistent
  volume is created named `ollama_data`.
- **SC-003**: Smoke test (`scripts/smoke_test.sh`) returns a non-empty `answer` when backend is configured to Ollama.
- **SC-004**: README contains the exact `docker exec` command to pull/run `llama3` inside the container.

## Assumptions

- The project uses Gradle or Maven; changes will be applied to the appropriate build file (build.gradle or pom.xml).
- The chosen default model is `llama3` (lightweight); if unavailable, `mistral` is an acceptable alternative.
- Local developers have Docker and docker-compose available to run Ollama.

## Notes

- If the current code references NVIDIA-specific classes, they will be refactored to use generic interfaces.
- License and provenance for any model used with Ollama must be documented in README.

## Done When

- All functional requirements are implemented and verified by the success criteria above.
- Spec validated by the Specification Quality Checklist in checklists/requirements.md
