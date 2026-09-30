# API Requirements Quality Checklist: token-usage-metrics

**Purpose**: Validate that the token-usage response requirements are complete, clear, consistent, measurable, and ready for implementation review.
**Created**: 2026-09-30
**Feature**: [spec.md](../spec.md)
**Audience**: Reviewer (PR/planning)
**Depth**: Standard requirements-quality review

## Requirement Completeness

- [x] CHK001 - Does the specification enumerate every preserved response field and every newly required metric field without relying on an implicit DTO definition? [Completeness, Spec §FR-002, §FR-003]
- [x] CHK002 - Does the contract define successful, validation-error, model-failure, and unexpected-error response requirements, including their status and correlation behavior? [Completeness, Spec §FR-007, Edge Cases]
- [x] CHK003 - Does the specification explicitly identify every response-construction path that must remain aligned when the response shape changes? [Completeness, Spec §FR-008]
- [x] CHK004 - Are requirements provided for both provider metadata being present and provider metadata being partially or entirely absent? [Completeness, Spec §FR-004, §FR-007, Edge Cases]

## Requirement Clarity

- [x] CHK005 - Is the source of each metric explicitly distinguished between provider metadata, result metadata, and locally calculated values? [Clarity, Spec §FR-004, §FR-006]
- [x] CHK006 - Is the `tokensPerSecond` formula unambiguous about numerator, denominator, units, and invalid-input behavior? [Clarity, Spec §FR-006, §SC-003]
- [x] CHK007 - Is `null` defined as the sole representation for unavailable metrics, with zero clearly reserved for an actual provider value? [Clarity, Spec §FR-007, Assumptions]
- [x] CHK008 - Is the meaning of a provider-reported `totalTokens` value clear when it differs from the sum of component counts? [Clarity, Spec §FR-005, Edge Cases]
- [x] CHK009 - Are the terms “elapsed time,” “output tokens,” “finish reason,” “model,” and “unavailable” defined consistently across requirements, scenarios, and success criteria? [Clarity, Spec §FR-003, §FR-006, Key Entities]

## Requirement Consistency

- [x] CHK010 - Are the requirements for always-present metric fields with `null` values consistent between successful responses, error responses, assumptions, and edge cases? [Consistency, Spec §FR-003, §FR-007, Assumptions]
- [x] CHK011 - Do the requirements consistently preserve the provider’s `totalTokens` while still requiring comparison with `promptTokens + completionTokens`? [Consistency, Spec §FR-005, §SC-002]
- [x] CHK012 - Do compatibility requirements preserve the existing request shape and four response fields without accidentally requiring a new endpoint or request parameter? [Consistency, Spec §FR-001, §FR-002, Out of Scope]
- [x] CHK013 - Are privacy requirements consistent between the user scenarios, functional requirements, and success criteria? [Consistency, Spec User Story 2, §FR-009, §SC-005]

## Acceptance Criteria Quality

- [x] CHK014 - Can a reviewer objectively determine whether each successful response contains the four preserved fields and six new fields? [Measurability, Spec §SC-001]
- [x] CHK015 - Can a reviewer objectively determine whether throughput matches the stated formula within +/-0.01 tokens per second after rounding to two decimal places? [Measurability, Spec §SC-003]
- [x] CHK016 - Can the requirement that provider values are not estimated or normalized be distinguished from the requirement to compare component totals? [Measurability, Spec §SC-002]
- [x] CHK017 - Does the log-privacy criterion define a zero-tolerance outcome for questions, answers, tokenized text, and personal information? [Measurability, Spec §SC-005]
- [x] CHK018 - Does the compatibility criterion identify the exact legacy field set that must remain consumable by existing clients? [Measurability, Spec §SC-006]

## Scenario and Edge Case Coverage

- [x] CHK019 - Are primary success, alternate metadata availability, exception/error, recovery/compatibility, and non-functional privacy scenarios all represented or intentionally bounded? [Coverage, Spec User Stories 1-2, Edge Cases]
- [x] CHK020 - Are zero, negative, unknown, and unavailable elapsed-time conditions distinguished sufficiently to prevent an ambiguous throughput requirement? [Edge Case, Spec Edge Cases, §FR-006]
- [x] CHK021 - Does the specification address missing finish reasons without treating an otherwise valid model response as a failure? [Edge Case, Spec Edge Cases]
- [x] CHK022 - Does the specification address provider total-count discrepancies without silently choosing a locally derived value? [Edge Case, Spec §FR-005]
- [x] CHK023 - Are clients that ignore newly added fields explicitly covered as a compatibility scenario rather than assumed compatible? [Coverage, Spec User Story 2, §SC-006]

## Non-Functional Requirements

- [x] CHK024 - Are privacy requirements specific enough to distinguish permitted aggregate diagnostics from prohibited question, answer, tokenized-text, and PII logging? [Security/Privacy, Spec §FR-009]
- [x] CHK025 - Does the specification define complete-flow test coverage as a requirement spanning client submission, model response metadata, returned JSON, errors, and log privacy? [Coverage, Spec §FR-010, §SC-004, §SC-005]
- [x] CHK026 - Is the requirement for real provider metadata compatible with the separate deterministic-test requirement, without allowing mocks to replace the end-to-end evidence? [Consistency, Spec §FR-004, Assumptions]
- [x] CHK027 - Are performance expectations limited to the stated throughput calculation and existing endpoint timing, without introducing unrequested sampling, memory, or optimization requirements? [Scope, Spec §FR-006, §FR-011, Out of Scope]

## Dependencies and Assumptions

- [x] CHK028 - Are the dependency on real model metadata and the fallback semantics for missing metadata stated as assumptions rather than left implicit? [Dependency/Assumption, Spec §FR-004, Assumptions]
- [x] CHK029 - Does the specification identify that all response constructors/builders, including error paths, must remain contract-complete when fields are added? [Dependency, Spec §FR-008, User Story 2]
- [x] CHK030 - Are the deterministic unit, endpoint-level, and live-provider test layers described as complementary evidence rather than interchangeable alternatives? [Dependency, Spec §FR-010, Assumptions]
- [x] CHK031 - Are the exclusions for sampling parameters, templates, memory, persistence, new endpoints, and later blocks explicit enough to prevent checklist scope creep? [Scope, Spec §FR-011, Out of Scope]

## Ambiguities and Conflicts

- [x] CHK032 - Is the distinction between the endpoint’s public `ChatResponse` and the model provider’s response metadata explicit enough to prevent terminology collisions? [Ambiguity, Spec Key Entities, Plan §Constraints]
- [x] CHK033 - Does the wording reconcile the requirement to preserve `totalTokens` with the phrase “comparable to the sum” without implying an equality guarantee the provider may not offer? [Ambiguity/Conflict, Spec §FR-005, User Story 1]
- [x] CHK034 - Does the specification distinguish a missing metric (`null`) from an actual provider-reported zero in every affected field? [Ambiguity, Spec §FR-007, Assumptions]
