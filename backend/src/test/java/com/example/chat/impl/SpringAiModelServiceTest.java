package com.example.chat.impl;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiModelServiceTest {

    @Test
    void mapsProviderUsageModelTextAndFinishReason() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        org.springframework.ai.chat.model.ChatResponse providerResponse = providerResponse(
                "answer", 12, 8, 21, "llama3", "stop");
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt().user("question").call().chatResponse()).thenReturn(providerResponse);

        SpringAiModelService service = new SpringAiModelService(builder);

        var response = service.generateResponse("question", "session");

        assertThat(response.answer()).isEqualTo("answer");
        assertThat(response.promptTokens()).isEqualTo(12);
        assertThat(response.completionTokens()).isEqualTo(8);
        assertThat(response.totalTokens()).isEqualTo(21);
        assertThat(response.model()).isEqualTo("llama3");
        assertThat(response.finishReason()).isEqualTo("stop");
    }

    @Test
    void mapsUnavailableProviderMetadataToNull() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        org.springframework.ai.chat.model.ChatResponse providerResponse = providerResponse(
                "answer", null, null, null, "llama3", null);
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt().user("question").call().chatResponse()).thenReturn(providerResponse);

        SpringAiModelService service = new SpringAiModelService(builder);

        var response = service.generateResponse("question", "session");

        assertThat(response.answer()).isEqualTo("answer");
        assertThat(response.promptTokens()).isNull();
        assertThat(response.completionTokens()).isNull();
        assertThat(response.totalTokens()).isNull();
        assertThat(response.model()).isEqualTo("llama3");
        assertThat(response.finishReason()).isNull();
    }

    private static org.springframework.ai.chat.model.ChatResponse providerResponse(
            String text,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            String model,
            String finishReason) {
        Generation generation = new Generation(
                new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build());
        return org.springframework.ai.chat.model.ChatResponse.builder()
                .generations(List.of(generation))
                .metadata(ChatResponseMetadata.builder()
                        .model(model)
                        .usage(promptTokens == null && completionTokens == null
                                ? null
                                : new DefaultUsage(promptTokens, completionTokens, totalTokens))
                        .build())
                .build();
    }
}
