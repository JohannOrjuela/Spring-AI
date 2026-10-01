# Data Model: Conversation Memory

## ChatRequest

The existing flat chat request remains the external input. Only the semantics and validation of `sessionId` change; all feature 004 and 005 fields remain as defined in their contracts.

| Field | Type | Required | Validation and semantics |
|---|---|---:|---|
| `sessionId` | string or null | No | Null/omitted is stateless. A supplied value must contain at least one non-whitespace character and is used exactly, case-sensitively, without trimming. |
| `question` | string | Yes | Nonblank current user content; it counts in the 20-message provider window. |
| `templateId` | string or null | No | Exact registered identifier; null/omitted resolves to `conciso`; remains request-scoped. |
| `rol`, `dominio`, `idioma` | string or null | No | Existing nonblank/default rules; remain request-scoped. |
| `temperature`, `topP`, `topK`, `numPredict`, `seed` | number or null | No | Existing feature 004 ranges, null/default semantics, and explicit-zero behavior. |

Unknown properties remain rejected before model invocation. No identifier is echoed in the response.

## ConversationSession

Runtime-only state for one exact nonblank identifier.

| Field | Type | Cardinality | Invariant |
|---|---|---:|---|
| `sessionId` | opaque string | 1 | Exact key; case-sensitive; never normalized or logged. |
| `messages` | ordered `ConversationMessage` list | 0..20 | Contains only committed user and assistant messages in chronological order. |
| `admissionGate` | runtime coordination primitive | 1 | Serializes the full turn lifecycle for this exact key; not persisted or exposed. |

Relationships:

- Each message belongs to exactly one session.
- Distinct exact identifiers never share messages or a gate.
- Session state exists only in the running backend process and disappears on restart.
- Active-session count has no TTL or management API in this demo; those concerns remain outside scope.

## ConversationMessage

| Field | Type | Validation and semantics |
|---|---|---|
| `role` | `USER` or `ASSISTANT` | System and error roles are never stored. |
| `content` | string | Nonblank successful conversational content. |
| `position` | implicit ordered index | Preserves chronological order within one session. |

Successful commits add one user message and one assistant message together. The durable history is then reduced to the newest 20 messages by removing from the head.

## CurrentSystemInstruction

Transient provider input rendered for one request.

| Field | Source | Lifecycle |
|---|---|---|
| `content` | Current registered template plus current `rol`, `dominio`, `idioma` | Created per request, placed first, never stored. |
| `templateId` | Current request/default | Not inherited by later turns. |

It is excluded from the 20-message count and cannot be evicted by conversation history.

## ProviderMessageWindow

Transient ordered provider input for a stateful request:

```text
current system instruction
  + newest retained user/assistant messages that fit
  + current user message
```

Invariants:

- Exactly one current system instruction appears first and is excluded from the count.
- The current user message is included in the non-system count.
- At most 20 non-system messages are sent.
- If retained history plus the current user exceeds 20, remove the oldest retained messages first.
- Stored history is not mutated until a nonblank provider answer succeeds.

## ChatResponse and Classification Contracts

`ChatResponse`, `ClassificationRequest`, `Clasificacion`, and `ClassificationResponse` retain their feature 003–005 fields, constraints, status codes, and nullability. Classification remains stateless and continues rejecting `sessionId` as an unknown property.

Provider `promptTokens` describes the actual prompt exposed by the provider, including conversation history when present. No new memory field is added to either response.

## ExperimentERun

Explicit local execution metadata, printed rather than persisted by default.

| Field | Type | Constraint |
|---|---|---|
| `sessionId` | generated synthetic string | One nonblank value reused by all turns. |
| `turnNumber` | integer | Exactly 1 through 20, sequential. |
| `prompt` | fixed synthetic string | No user or sensitive data. |
| `responseEnvelope` | existing `ChatResponse` | Must pass HTTP and contract validation. |
| `observation` | text/boolean summary | Continuity/eviction indicator; observational, not a semantic hard gate. |

## State transitions

```text
request received
  ├─ invalid question/session/template/context/sampling/unknown field
  │    └─ HTTP 400; no gate, model call, or memory mutation
  ├─ sessionId null or omitted
  │    └─ render current system + user → provider → existing response; no memory access
  └─ exact nonblank sessionId
       └─ acquire that session's fair gate
            └─ read snapshot → bound history + current user → render current system
                 ├─ provider exception or blank answer
                 │    └─ existing safe error; snapshot unchanged; release gate
                 └─ nonblank answer
                      └─ append user + assistant → evict oldest above 20
                           └─ return existing success + metrics; release gate
```

Restarting the backend discards every session, message, and gate. There is no persistence, clear/list/export endpoint, or cross-instance sharing.
