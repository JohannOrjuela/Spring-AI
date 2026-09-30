package com.example.chat;

public interface ModelService {
    record ModelResponse(
            String answer,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            String model,
            String finishReason) {
    }

    /**
     * Generate a response from the chosen model.
     * Implementations should handle communication with Spring AI / model client.
     */
    ModelResponse generateResponse(String question, String sessionId);
}
