# Research: spring-ai-demo

## Decisions

- Tech stack chosen: Java 25, Spring Boot 3.5, Spring AI, Angular 20.
  - Rationale: Aligns with user's explicit preference; Spring AI provides native
    integration points for LLMs and fits Java ecosystem. Angular 20 provides a
    modern, typed frontend framework suitable for a simple chat UI.
  - Alternatives considered: Node/Express + React (lighter for quick demos) but
    rejected because the project goal is to showcase Spring AI specifically.

## Model provenance & licensing

- Action: Verify NVIDIA model image license and record provenance in README.
- Recommendation: Use an official NVIDIA-provided model image or documented
  third-party image with permissive license. Avoid proprietary images without
  redistribution rights.

## Quickstart validation

- Validate that Docker-based local runtime can start model container, backend,
  and frontend with scripted commands. Provide smoke test to assert end-to-end
  flow.

## Security and secrets

- Secrets MUST be environment variables or use a local secrets manager.
- Logs must redact any PII or secret-like patterns.

## Conclusion
All research items are actionable. Proceed to design artifacts and quickstart creation.
