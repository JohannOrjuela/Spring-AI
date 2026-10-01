package com.example.chat;

import com.example.chat.dto.ChatRequest;
import com.example.chat.dto.ClassificationRequest;
import com.example.chat.dto.Clasificacion;

public interface ModelService {
    record ModelResponse(
            String answer,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            String model,
            String finishReason) {
    }

    record ClassificationModelResponse(
            Clasificacion clasificacion,
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
    ModelResponse generateResponse(ChatRequest request);

    ClassificationModelResponse classify(ClassificationRequest request);
}
