# Implementation Plan: migrate-to-ollama

**Feature Directory**: specs/002-migrate-to-ollama
**Spec**: specs/002-migrate-to-ollama/spec.md
**Created**: 2026-07-27

## Technical Context

- Goal: Replace existing Nvidia/OpenAI Spring AI integration with a local Ollama runtime.
- Target stack changes: backend build updated to include `spring-ai-ollama-spring-boot-starter`.
- Runtime: Ollama container (`ollama/ollama:latest`) exposing port 11434 and persistent volume `ollama_data`.
- Backend properties: `spring.ai.ollama.base-url` and `spring.ai.default-model` set in application.properties.

### Unknowns / NEEDS_CLARIFICATION
- Exact artifact coordinates for the Ollama starter dependency (groupId:artifactId:version). Plan uses placeholder `com.ollama:spring-ai-ollama-spring-boot-starter:0.1.0` — confirm preferred vendor/version.
- Confirm whether backend should point to `http://localhost:11434` (host machine) or `http://ollama:11434` when running inside docker-compose for correct intra-network routing. Default uses localhost as requested; Quickstart starts containers so tests may use the container host mapping.

## Constitution Check

Per .specify/memory/constitution.md the migration MUST verify:
- Reproducible local execution: Quickstart will include Docker Compose with Ollama and validation steps.
- Clear Interfaces & Contracts: Existing chat API contract will remain and backend will use ChatModel abstraction.
- Data Minimization & Secrets Handling: No new secrets introduced; if required document secure handling.
- Testing & Observability: Smoke test updated to validate Ollama integration.

Gates: Phase 0 MUST document research.md; Phase 1 MUST produce updated docker-compose and application.properties.

---

## Phase 0: Research

Tasks:
- Verify Ollama image licensing and model availability for `llama3`/`mistral`.
- Confirm starter dependency coordinates and recommended version.
- Document expected Ollama API surface for the chosen integration approach.

Output: specs/002-migrate-to-ollama/research.md

## Phase 1: Design & Contracts

Deliverables:
- specs/002-migrate-to-ollama/data-model.md (minimal; migration-focused)
- specs/002-migrate-to-ollama/contracts/notes.md (integration notes)
- specs/002-migrate-to-ollama/quickstart.md (updated quickstart using docker compose)

Key tasks:
- Update docker/docker-compose.yml (add ollama service + volume)
- Update backend build file to include the Ollama starter and remove Nvidia/OpenAI starter
- Add application.properties entries for Ollama base URL and default model
- Refactor Java beans to use ChatModel/OllamaChatModel (where Nvidia-specific imports existed)

## Phase 2: Tasks & Implementation

- Generate tasks.md for migration details (not created here).
- Implement code changes and update documentation.

---

## Artifacts Generated
- plan: specs/002-migrate-to-ollama/plan.md
- research: specs/002-migrate-to-ollama/research.md
- data model: specs/002-migrate-to-ollama/data-model.md
- contracts: specs/002-migrate-to-ollama/contracts/notes.md
- quickstart: specs/002-migrate-to-ollama/quickstart.md

## Next Steps
- Confirm dependency coordinates for Ollama starter (or accept placeholder)
- Execute migration tasks and run smoke test to validate
