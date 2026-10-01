package com.example.chat.dto;

import java.util.List;

public record ClassificationResponse(
        String requestId, Clasificacion classification, String answer, long elapsedMs, String status,
        Integer promptTokens, Integer completionTokens, Integer totalTokens,
        String model, String finishReason, Double tokensPerSecond,
        List<ChatResponse.ValidationError> errors) {
}
