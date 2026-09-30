package com.example.chat.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ChatResponseTest {

        private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void preservesExistingFieldsAndSerializesAllMetrics() throws Exception {
        ChatResponse response = new ChatResponse(
                "request-1",
                "answer",
                123,
                "ok",
                12,
                8,
                20,
                "llama3",
                "stop",
                6.5);

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains(
                "\"requestId\":\"request-1\"",
                "\"answer\":\"answer\"",
                "\"elapsedMs\":123",
                "\"status\":\"ok\"",
                "\"promptTokens\":12",
                "\"completionTokens\":8",
                "\"totalTokens\":20",
                "\"model\":\"llama3\"",
                "\"finishReason\":\"stop\"",
                "\"tokensPerSecond\":6.5");
    }

    @Test
    void keepsEmptyConstructorForJacksonAndNullMetrics() throws Exception {
        ChatResponse response = objectMapper.readValue(
                "{\"requestId\":\"request-1\",\"answer\":\"\",\"elapsedMs\":0,\"status\":\"error\","
                        + "\"promptTokens\":null,\"completionTokens\":null,\"totalTokens\":null,"
                        + "\"model\":null,\"finishReason\":null,\"tokensPerSecond\":null}",
                ChatResponse.class);

        assertThat(response.getRequestId()).isEqualTo("request-1");
        assertThat(response.getPromptTokens()).isNull();
        assertThat(response.getCompletionTokens()).isNull();
        assertThat(response.getTotalTokens()).isNull();
        assertThat(response.getModel()).isNull();
        assertThat(response.getFinishReason()).isNull();
        assertThat(response.getTokensPerSecond()).isNull();
    }
}
