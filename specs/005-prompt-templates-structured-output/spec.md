# Feature Specification: Prompt Templates and Structured Output

**Feature Branch**: `005-prompt-templates-structured-output`

**Created**: 2026-09-30

**Status**: Draft

**Input**: User description: "Crear la feature 005-prompt-templates-structured-output para extraer los prompts de sistema del código Java, permitir seleccionar plantillas registradas con variables controladas y agregar una operación de clasificación estructurada, conservando las features 003 y 004 y excluyendo los experimentos."

## Clarifications

### Session 2026-09-30

- Q: ¿La clasificación estructurada utilizará un endpoint dedicado o un modo del endpoint de chat? → A: Endpoint dedicado `POST /api/v1/classifications`.
- Q: ¿Qué valores se usan cuando `rol`, `dominio` o `idioma` no son enviados? → A: `rol="asistente"`, `dominio="general"` e idioma de la pregunta.
- Q: ¿Qué variables admite cada plantilla registrada? → A: Las tres admiten exclusivamente `rol`, `dominio`, `idioma` y la variable común `question`; en clasificación, el campo externo `text` se mapea internamente a `question` antes del renderizado.
- Q: ¿Cómo se garantiza la salida estructurada de clasificación? → A: Se combinan la salida estructurada nativa del proveedor y la validación contra el esquema requerido.
- Q: ¿Cómo conserva la clasificación los parámetros de muestreo y las métricas existentes? → A: Acepta los cinco parámetros de muestreo y devuelve `classification` junto con todas las métricas existentes.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Select a registered prompt template (Priority: P1)

As a chat client, I want to select a known prompt template and supply its supported context values so that I can change the assistant's behavior per request without changing or redeploying application logic.

**Why this priority**: Moving prompt behavior out of a fixed application constant and making it selectable is the primary value of the feature.

**Independent Test**: Send otherwise identical chat requests using each registered identifier and verify that all are accepted, that the selected behavior is applied only to that request, and that omitting the identifier preserves the concise default behavior.

**Acceptance Scenarios**:

1. **Given** the registered templates `conciso`, `tutor`, and `extractor`, **When** a client sends a chat request with one of those identifiers, **Then** the system uses that template for that request.
2. **Given** a valid chat request with no template identifier or an explicit null identifier, **When** the request is processed, **Then** the system uses `conciso`.
3. **Given** a registered template and supported values for role, domain, and language, **When** the client submits the request, **Then** those values are applied only to the corresponding controlled template variables.
4. **Given** a template that contains a literal JSON example, **When** it is rendered with its supported variables, **Then** the JSON structure remains literal and the variables are substituted correctly.

---

### User Story 2 - Receive a structured classification (Priority: P2)

As an application client, I want to classify text into a predictable structure so that software can consume the result without interpreting free-form prose.

**Why this priority**: A stable structure makes model output usable by application logic while remaining independent from the general chat flow.

**Independent Test**: Submit representative text and valid sampling values to the classification operation and verify that every successful response contains a `classification` object with a nonblank category, an integer confidence value from 0 through 100, and a nonblank justification, together with all existing metrics.

**Acceptance Scenarios**:

1. **Given** valid text to classify, **When** the client requests a classification, **Then** the successful response contains a `classification` object with `categoria`, `confianza`, and `justificacion`, plus the existing request, status, latency, token, model, completion, and throughput fields.
2. **Given** a successful classification, **When** its result is inspected, **Then** all three fields are present, `categoria` and `justificacion` are nonblank, and `confianza` is between 0 and 100 inclusive.
3. **Given** model output that cannot satisfy the required structure, **When** the system cannot produce a valid classification, **Then** it returns the existing safe error contract instead of an incomplete success response.

---

### User Story 3 - Reject unsafe template control without regressions (Priority: P3)

As the application owner, I want clients limited to registered template identifiers so that they cannot replace the system instructions, access arbitrary resources, or bypass the established request contract.

**Why this priority**: Template selection must not turn into unrestricted system-prompt control, and the existing metrics and sampling features must continue to work.

**Independent Test**: Submit unknown identifiers, prompt-like text, valid sampling parameters, invalid sampling parameters, and ordinary legacy requests; verify safe rejection where required and unchanged behavior for existing clients.

**Acceptance Scenarios**:

1. **Given** an unregistered template identifier, **When** a client submits a request, **Then** the system returns HTTP 400 with a comprehensible field error and does not invoke the model.
2. **Given** text that resembles a system prompt, file path, or resource location in the template identifier, **When** the request is submitted, **Then** it is treated only as an unknown identifier and rejected without resolving or executing it.
3. **Given** a valid registered template and valid sampling parameters, **When** the request succeeds, **Then** the selected template and sampling values apply to the same request and the complete existing metrics remain available.
4. **Given** an existing client that sends no new template fields, **When** it sends a valid chat request, **Then** its status, errors, metrics, nullability, and default sampling behavior remain compatible.
5. **Given** a request containing any property outside the documented request contract, including a client-controlled system prompt, resource, path, or template definition, **When** it is submitted, **Then** the system returns HTTP 400 before invoking the model.

### Edge Cases

- `templateId` is omitted, explicitly null, blank, uses different letter casing, or contains leading/trailing whitespace.
- One or more optional context values are omitted, null, blank, or unusually long; this feature rejects blank values but introduces no maximum length, so any nonblank value remains accepted.
- A context value contains template-like delimiter characters or JSON punctuation.
- A registered template contains literal JSON objects and nested braces.
- A request combines a registered template with explicit zero sampling values.
- The model returns valid JSON with missing, null, wrongly typed, or out-of-range classification fields.
- The model is unavailable before a classification can be produced.
- Concurrent requests select different templates and context values.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST maintain editable prompt definitions separately from executable application logic.
- **FR-002**: The initial registered prompt identifiers MUST be exactly `conciso`, `tutor`, and `extractor`.
- **FR-003**: Clients MUST be able to select a registered prompt by sending its identifier with a chat request.
- **FR-004**: The system MUST use `conciso` when the template identifier is omitted or explicitly null.
- **FR-005**: The system MUST reject a blank or unregistered template identifier with HTTP 400 before invoking the model.
- **FR-006**: Chat and classification requests MUST reject every unknown JSON property with HTTP 400 before model invocation. This includes properties attempting to provide system-prompt text, prompt paths, resource locations, or unregistered template definitions; unknown properties MUST NOT be silently accepted or ignored.
- **FR-007**: Chat and classification requests MUST support optional role, domain, and language context values while retaining their existing request fields; omitted or null values MUST use `asistente`, `general`, and the language of the user content, respectively.
- **FR-008**: Each registered template MUST support exactly the controlled variables `rol`, `dominio`, `idioma`, and `question`; chat supplies `question` directly and classification maps its external `text` field to that common internal variable. The system MUST NOT interpolate additional client-provided variable names.
- **FR-009**: Template rendering MUST preserve literal JSON syntax while substituting supported variables without ambiguity.
- **FR-010**: Values supplied for one request MUST NOT alter the template, context, sampling behavior, or defaults of another sequential or concurrent request.
- **FR-011**: The system MUST provide classification through the dedicated operation `POST /api/v1/classifications`, whose successful response contains a `classification` object with the required fields `categoria`, `confianza`, and `justificacion`.
- **FR-012**: A successful classification MUST contain a nonblank category, an integer confidence from 0 through 100 inclusive, and a nonblank justification.
- **FR-013**: The system MUST request provider-enforced structured output, validate the returned JSON against the required schema, and explicitly apply Jakarta Bean Validation to the converted `Clasificacion`; it MUST NOT report success when a required field is missing, blank, null, incorrectly typed, or has an out-of-range confidence value.
- **FR-014**: Validation and classification failures MUST use the established response contract, including a generated nonblank request identifier, error status, stable general message, nullable metrics, and safe field-level details when applicable.
- **FR-015**: Both chat and classification requests MUST accept the five existing sampling parameters with their current validation ranges, omission semantics, and explicit-zero behavior.
- **FR-016**: Existing response metrics and their nullability MUST remain available for chat responses produced from registered templates and for classification responses alongside the `classification` object.
- **FR-017**: Existing clients that omit all new fields MUST remain compatible and receive the concise default behavior.
- **FR-018**: Operational logs MUST NOT include raw questions, answers, rejected prompt-like values, rendered system prompts, or classification text.
- **FR-019**: The primary chat and classification flows MUST have explicit request/response examples and automated coverage from client input through the model boundary and returned response.
- **FR-020**: This feature MUST NOT automatically execute experiments, repeat classification requests, calculate comparative measurements or costs, or generate experiment tables, charts, and conclusions.

### Key Entities

- **Prompt Template**: A server-controlled, editable prompt definition identified by a public identifier and containing exactly the supported variables `rol`, `dominio`, `idioma`, and `question`; classification maps its external `text` into `question`.
- **Template Selection**: The identifier and optional role, domain, and language values associated with one chat request; it never contains system-prompt text.
- **Classification Request**: The user-provided text, optional role, domain, and language context, and the five existing optional sampling controls needed to request one structured classification.
- **Classification**: A structured result containing a required category, confidence from 0 through 100, and justification.
- **Classification Response**: A response envelope containing request and status information, a `classification` object on success, and all existing latency, token, model, completion, and throughput metrics with their established nullability.
- **Validation Error**: A safe field and message pair returned when a request cannot be accepted without exposing its rejected value.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All three registered identifiers can be selected independently, and 100% of valid requests use only the template selected for that request.
- **SC-002**: 100% of requests with an omitted or null template identifier use `conciso` without requiring client changes.
- **SC-003**: 100% of blank, unknown, path-like, and prompt-like template identifiers, plus every JSON property outside the documented chat or classification contract, are rejected before model invocation with a safe HTTP 400 response.
- **SC-004**: 100% of successful classification responses contain a `classification` object with the three required fields and documented constraints, plus every existing metric field.
- **SC-005**: Template definitions containing representative literal JSON objects render without corrupting the JSON or confusing literal braces with variables.
- **SC-006**: Automated regression coverage confirms that all existing sampling boundaries, explicit-zero cases, metrics, nullable error fields, and legacy requests retain their prior behavior.
- **SC-007**: Sequential and concurrent automated checks show zero cross-request leakage of template selections or context values.
- **SC-008**: Automated end-to-end verification covers both a template-selected chat and a structured classification through the existing local demonstration environment.

## Assumptions

- `conciso` preserves the current concise assistant behavior and is the backward-compatible default.
- Template identifiers are case-sensitive exact values; surrounding whitespace is not silently normalized.
- When role, domain, or language is absent, the selected template uses `asistente`, `general`, and the language of the user's question, respectively.
- Context fields retain `null` in the deserialized DTO and are normalized only when building the request-scoped prompt; explicit blank values are invalid, while this feature defines no maximum length for nonblank context.
- Classification confidence uses a conventional whole-number scale from 0 to 100 inclusive.
- Classification uses the dedicated `POST /api/v1/classifications` operation, separate from ordinary free-form chat, so that each response contract remains predictable.
- Prompt-template changes may require rebuilding or restarting the packaged application, but they do not require editing executable application logic.
- The existing local model, container topology, frontend, versioned contract, error behavior, sampling controls, and metric fields remain dependencies of this feature.
- Reliability experiments, repeated trials, comparative token/latency measurements, costs, charts, and manual quality conclusions are intentionally outside this feature.
