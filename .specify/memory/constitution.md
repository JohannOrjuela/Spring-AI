<!--
Sync Impact Report
Version change: [CONSTITUTION_VERSION] -> 0.1.0
Modified principles:
  - [PRINCIPLE_1_NAME] -> Demo-Focused Simplicity
  - [PRINCIPLE_2_NAME] -> Reproducible Local Execution
  - [PRINCIPLE_3_NAME] -> Clear Interfaces & Contracts
  - [PRINCIPLE_4_NAME] -> Data Minimization & Secrets Handling
  - [PRINCIPLE_5_NAME] -> Testing & Observability
Added sections:
  - Operational Constraints
  - Development Workflow
Removed sections: none
Templates requiring updates:
  - .specify/templates/plan-template.md ⚠ pending
  - .specify/templates/spec-template.md ⚠ pending
  - .specify/templates/tasks-template.md ⚠ pending
  - .specify/templates/commands/* ⚠ missing (directory not found)
Follow-up TODOs:
  - Review and optionally update templates listed above to reflect new constitution gates and wording.
-->

# Pruebachat Constitution

## Core Principles

### Demo-Focused Simplicity
The project MUST remain small, focused, and demonstrable. Features are evaluated by whether
they complete an end-to-end demonstration of Spring AI integration with the Angular UI and
local model container. Avoid adding non-essential services or production-scale infrastructure
that do not directly improve the demo's clarity or reproducibility.

### Reproducible Local Execution
The system MUST be runnable locally using Docker (docker-compose or equivalent). The
repository MUST include a Quickstart that launches the backend, model container, and
frontend with documented environment variables. Developers MUST be able to reproduce the
demo on a machine with the documented minimal hardware (GPU optional; document when
required).

### Clear Interfaces & Contracts
All inter-component communication (frontend ↔ backend ↔ model) MUST use explicit,
versioned HTTP/JSON contracts. Primary user flows MUST have example requests/responses
and automated acceptance tests that validate the demo's core path.

### Data Minimization & Secrets Handling
User inputs and logs MUST avoid persisting sensitive data. Secrets (API keys, credentials)
MUST be stored in environment variables or a secrets mechanism documented in the Quickstart
and never committed. Logging MUST sanitize or redact PII before persistence.

### Testing & Observability
Core flows (frontend input → backend → model → frontend response) MUST be covered by
automated tests (unit + integration/acceptance). The Quickstart verification step MUST run a
small smoke test to confirm end-to-end functionality. Structured logs and basic metrics
(MODEL_UP, REQUEST_SUCCESS, REQUEST_LATENCY_MS) SHOULD be exposed for debugging.

## Operational Constraints
The project is a demo and MUST not assume cloud-only infrastructure. Requirements:
- Provide Docker container(s) for the local model and any supporting services.
- Document GPU requirements; mark GPU-accelerated features as optional and provide CPU
  fallbacks where practical.
- License and model provenance MUST be documented for any third-party model images used.
- Quickstart MUST include commands to build and run the full demo locally.

## Development Workflow
- Branching: use feature branches for changes; PRs are required for merging to main.
- Code review: at least one approving review is required for non-trivial changes.
- Tests: PRs that change core flows MUST include or update unit and acceptance tests;
  the CI gate MUST run the Quickstart smoke test (or a CI-appropriate simulation).
- Commit message convention: keep concise, reference related spec/plan; include the
  Co-authored-by trailer mandated by repository policy.

## Governance
Amendments to this constitution require a documented PR describing the change, the
migration plan (if needed), and at least one approving maintainer review.

Versioning policy:
- MAJOR: Breaking redefinition or removal of core principles or governance
- MINOR: Addition of a new principle or material expansion of guidance
- PATCH: Wording clarifications, typo fixes, or non-semantic refinements

Compliance and enforcement:
- New plans must include a Constitution Check (see .specify/templates/plan-template.md).
- Project maintainers are responsible for ensuring plans and specs reference current
  constitution rules during reviews.

**Version**: 0.1.0 | **Ratified**: 2026-07-27 | **Last Amended**: 2026-07-27
