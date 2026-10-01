package com.example.chat.impl;

import com.example.chat.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.ollama.api.OllamaChatOptions;

import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpringAiModelServiceTest {

    @Test
    void mapsProviderUsageModelTextAndFinishReason() {
        Harness harness = harness(providerResponse("answer", 12, 8, 21, "llama3", "stop"));

        var response = harness.service.generateResponse(request("question"));

        assertThat(response.answer()).isEqualTo("answer");
        assertThat(response.promptTokens()).isEqualTo(12);
        assertThat(response.completionTokens()).isEqualTo(8);
        assertThat(response.totalTokens()).isEqualTo(21);
        assertThat(response.model()).isEqualTo("llama3");
        assertThat(response.finishReason()).isEqualTo("stop");
    }

    @Test
    void appliesAllSamplingValuesToOneRequest() {
        Harness harness = harness(providerResponse("answer", 1, 1, 2, "model", "stop"));
        ChatRequest request = request("question");
        request.setTemperature(0.7);
        request.setTopP(0.9);
        request.setTopK(40);
        request.setNumPredict(50);
        request.setSeed(7);

        harness.service.generateResponse(request);

        OllamaChatOptions options = capturedOptions(harness.requestSpec);
        assertThat(options.getTemperature()).isEqualTo(0.7);
        assertThat(options.getTopP()).isEqualTo(0.9);
        assertThat(options.getTopK()).isEqualTo(40);
        assertThat(options.getNumPredict()).isEqualTo(50);
        assertThat(options.getSeed()).isEqualTo(7);
    }

    @Test
    void appliesOnlyTemperatureAndTopKForPartialRequest() {
        Harness harness = harness(providerResponse("answer", 1, 1, 2, "model", "stop"));
        ChatRequest request = request("question");
        request.setTemperature(0.3);
        request.setTopK(5);

        harness.service.generateResponse(request);

        OllamaChatOptions options = capturedOptions(harness.requestSpec);
        assertThat(options.getTemperature()).isEqualTo(0.3);
        assertThat(options.getTopK()).isEqualTo(5);
        assertThat(options.getTopP()).isNull();
        assertThat(options.getNumPredict()).isNull();
        assertThat(options.getSeed()).isNull();
    }

    @Test
    void appliesOnlyTopPNumPredictAndSeedForAnotherPartialRequest() {
        Harness harness = harness(providerResponse("answer", 1, 1, 2, "model", "stop"));
        ChatRequest request = request("question");
        request.setTopP(0.5);
        request.setNumPredict(200);
        request.setSeed(-9);

        harness.service.generateResponse(request);

        OllamaChatOptions options = capturedOptions(harness.requestSpec);
        assertThat(options.getTemperature()).isNull();
        assertThat(options.getTopP()).isEqualTo(0.5);
        assertThat(options.getTopK()).isNull();
        assertThat(options.getNumPredict()).isEqualTo(200);
        assertThat(options.getSeed()).isEqualTo(-9);
    }

    @Test
    void mapsUnavailableProviderMetadataToNull() {
        Harness harness = harness(providerResponse("answer", null, null, null, "llama3", null));

        var response = harness.service.generateResponse(request("question"));

        assertThat(response.answer()).isEqualTo("answer");
        assertThat(response.promptTokens()).isNull();
        assertThat(response.completionTokens()).isNull();
        assertThat(response.totalTokens()).isNull();
        assertThat(response.model()).isEqualTo("llama3");
        assertThat(response.finishReason()).isNull();
    }

    @Test
    void omittedAndExplicitNullValuesPreserveConfiguredDefaults() {
        Harness omitted = harness(providerResponse("answer", 1, 1, 2, "model", "stop"));
        omitted.service.generateResponse(request("question"));
        verify(omitted.requestSpec, never()).options(any(OllamaChatOptions.Builder.class));

        Harness explicitNull = harness(providerResponse("answer", 1, 1, 2, "model", "stop"));
        ChatRequest request = request("question");
        request.setTemperature(null);
        request.setTopP(null);
        request.setTopK(null);
        request.setNumPredict(null);
        request.setSeed(null);
        explicitNull.service.generateResponse(request);
        verify(explicitNull.requestSpec, never()).options(any(OllamaChatOptions.Builder.class));
    }

    @Test
    void forwardsPermittedExplicitZeroValues() {
        Harness harness = harness(providerResponse("answer", 1, 1, 2, "model", "stop"));
        ChatRequest request = request("question");
        request.setTemperature(0.0);
        request.setTopP(0.0);
        request.setTopK(0);
        request.setSeed(0);

        harness.service.generateResponse(request);

        OllamaChatOptions options = capturedOptions(harness.requestSpec);
        assertThat(options.getTemperature()).isZero();
        assertThat(options.getTopP()).isZero();
        assertThat(options.getTopK()).isZero();
        assertThat(options.getSeed()).isZero();
        assertThat(options.getNumPredict()).isNull();
    }

    @Test
    void sequentialRequestsUseIndependentBuilders() {
        Harness harness = harness(providerResponse("answer", 1, 1, 2, "model", "stop"));
        ChatRequest first = request("first");
        first.setTemperature(0.2);
        ChatRequest second = request("second");
        second.setTopK(99);

        harness.service.generateResponse(first);
        harness.service.generateResponse(second);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(OllamaChatOptions.Builder.class);
        verify(harness.requestSpec, times(2)).options(captor.capture());
        List<OllamaChatOptions> options = captor.getAllValues().stream().map(OllamaChatOptions.Builder::build).toList();
        assertThat(options.get(0).getTemperature()).isEqualTo(0.2);
        assertThat(options.get(0).getTopK()).isNull();
        assertThat(options.get(1).getTemperature()).isNull();
        assertThat(options.get(1).getTopK()).isEqualTo(99);
    }

    @Test
    void concurrentRequestsUseIndependentBuilders() throws Exception {
        Harness harness = harness(providerResponse("answer", 1, 1, 2, "model", "stop"));
        ChatRequest first = request("first");
        first.setTemperature(0.4);
        ChatRequest second = request("second");
        second.setTemperature(1.4);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstCall = executor.submit(() -> harness.service.generateResponse(first));
            var secondCall = executor.submit(() -> harness.service.generateResponse(second));
            firstCall.get();
            secondCall.get();
        }

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(OllamaChatOptions.Builder.class);
        verify(harness.requestSpec, times(2)).options(captor.capture());
        assertThat(captor.getAllValues().stream().map(builder -> builder.build().getTemperature()).toList())
                .containsExactlyInAnyOrder(0.4, 1.4);
    }

    private static OllamaChatOptions capturedOptions(ChatClient.ChatClientRequestSpec requestSpec) {
        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(OllamaChatOptions.Builder.class);
        verify(requestSpec).options(captor.capture());
        return captor.getValue().build();
    }

    private static ChatRequest request(String question) {
        ChatRequest request = new ChatRequest();
        request.setQuestion(question);
        request.setSessionId("session");
        return request;
    }

    private static Harness harness(org.springframework.ai.chat.model.ChatResponse providerResponse) {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callSpec = mock(ChatClient.CallResponseSpec.class);
        when(builder.defaultSystem(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.templateRenderer(any())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any(OllamaChatOptions.Builder.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(providerResponse);
        return new Harness(new SpringAiModelService(builder, new com.example.chat.prompt.PromptTemplateRegistry(),
                jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator()), requestSpec);
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

    private record Harness(SpringAiModelService service, ChatClient.ChatClientRequestSpec requestSpec) {
    }
}
