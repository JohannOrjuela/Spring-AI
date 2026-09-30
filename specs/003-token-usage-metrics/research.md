# Research: Token Usage Metrics

## Decision: Preserve the existing endpoint and enrich the internal model result

**Rationale**: `POST /api/v1/chat` already owns request correlation, status, and the public DTO. A metadata-bearing internal result lets the service return answer text plus usage/model/finish metadata without adding an endpoint or changing the request payload.

**Alternatives considered**: A second metrics endpoint was rejected because it would split one chat operation across contracts. Recomputing usage from text was rejected because the feature requires real Spring AI/Ollama metadata.

## Diagnosis: Existing response paths and provider API

The current implementation has one `content()` call in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java` and three project-DTO constructor calls in `ChatController.java` and `GlobalExceptionHandler.java`. The current service therefore discards all provider response metadata before the controller builds its response.

Spring AI 2.0.0 confirms that `ChatClient.CallResponseSpec.chatResponse()` returns the fully qualified `org.springframework.ai.chat.model.ChatResponse`. Its metadata exposes `getUsage()` and `getModel()`, its result exposes the output text, and result metadata exposes `getFinishReason()`. `Usage` exposes `getPromptTokens()`, `getCompletionTokens()`, and `getTotalTokens()`.

## Decision: Use Spring AI `ChatResponse` metadata from `call().chatResponse()`

**Rationale**: The current service calls `content()`, which discards the response metadata. The requested Spring AI flow exposes `response.getMetadata().getUsage()`, `response.getMetadata().getModel()`, `response.getResult().getOutput().getText()`, and the result metadata finish reason. The fully qualified Spring AI type avoids collision with `com.example.chat.dto.ChatResponse`.

**Alternatives considered**: Keeping `content()` and estimating tokens was rejected because it cannot satisfy provider-authenticated usage. Adding a provider-specific HTTP client was rejected because the existing Spring AI/Ollama integration already supplies the needed data.

## Decision: Measure elapsed time with `System.nanoTime`

**Rationale**: A monotonic clock is appropriate for elapsed-duration measurement and avoids wall-clock adjustments. Convert the duration to milliseconds for the existing `elapsedMs` field, then compute `tokensPerSecond` from completion tokens and that value in seconds.

**Alternatives considered**: `Instant.now()`/epoch milliseconds was rejected for duration measurement because wall-clock changes can distort elapsed time. A second provider timing signal was rejected because the clarified contract uses the existing endpoint `elapsedMs`.

## Decision: Represent unavailable metrics as JSON `null`

**Rationale**: Always emitting the fields keeps the response shape stable for clients while distinguishing unavailable data from zero. This applies to success responses lacking provider metadata and to error responses with no model response.

**Alternatives considered**: Omitting fields creates variable response shapes. Zero or empty-string defaults would fabricate a metric and make unavailable data indistinguishable from a real zero.

## Decision: Preserve provider `totalTokens`

**Rationale**: The provider response is the source of truth. Returning its total allows direct comparison with prompt plus completion counts and exposes discrepancies instead of silently normalizing them.

**Alternatives considered**: Replacing the provider total with a local sum would violate the requirement to use real metadata. Rejecting a response for a metadata discrepancy would make otherwise valid chat responses fail.

## Decision: Two-layer test strategy

**Rationale**: Unit tests can deterministically cover null handling, arithmetic, metadata mapping, and error construction. MockMvc tests verify the public JSON contract and compatibility. A live Ollama flow verifies that real Spring AI/Ollama metadata reaches the endpoint.

**Alternatives considered**: Only live tests would be slow and fragile for arithmetic/error cases. Only mocks would not prove the provider metadata path.

## Decision: Remove user content from application logs

**Rationale**: The current controller logs the question and the response DTO can expose the answer. The feature and constitution require data minimization. Logs should retain only non-sensitive request correlation and aggregate diagnostics such as status, elapsed time, model, and counts.

**Alternatives considered**: Relying only on the existing servlet filter was rejected because it logs request metadata but does not prevent explicit controller logging of question/answer content.

## Open implementation notes for tasks

- Confirm the exact nullable types/accessors available in the Spring AI 2.0.0 API while implementing and adapt only the mapping code if the library represents absent metadata differently.
- Keep the project DTO name unchanged and use the fully qualified Spring AI `ChatResponse` at the collision point.
- Update every constructor call, including validation and global exception paths, so all metric fields serialize as `null` when unavailable.
