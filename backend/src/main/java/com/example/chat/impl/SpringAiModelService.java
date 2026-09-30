package com.example.chat.impl;

import com.example.chat.ModelService;
import com.example.chat.dto.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

/**
 * Implementacion de ModelService sobre Spring AI.
 *
 * El starter spring-ai-starter-model-ollama autoconfigura el bean ChatClient.Builder
 * a partir de las propiedades spring.ai.ollama.*, por lo que aqui no hace falta
 * construir clientes HTTP, ni resolver endpoints, ni usar reflexion.
 */
@Service
public class SpringAiModelService implements ModelService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiModelService.class);

    private static final String SYSTEM_PROMPT = """
            Eres un asistente conciso y directo.
            Responde siempre en el mismo idioma en el que te escriba el usuario.
            Si no sabes algo, dilo en lugar de inventar.
            """;

    private final ChatClient chatClient;

    public SpringAiModelService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Override
    public ModelResponse generateResponse(ChatRequest request) {
        log.debug("Enviando prompt al modelo");

        ChatClient.ChatClientRequestSpec prompt = chatClient.prompt()
                .user(request.getQuestion());

        if (hasSamplingOptions(request)) {
            OllamaChatOptions.Builder options = OllamaChatOptions.builder();
            if (request.getTemperature() != null) {
                options.temperature(request.getTemperature());
            }
            if (request.getTopP() != null) {
                options.topP(request.getTopP());
            }
            if (request.getTopK() != null) {
                options.topK(request.getTopK());
            }
            if (request.getNumPredict() != null) {
                options.numPredict(request.getNumPredict());
            }
            if (request.getSeed() != null) {
                options.seed(request.getSeed());
            }
            prompt = prompt.options(options);
        }

        org.springframework.ai.chat.model.ChatResponse response = prompt.call().chatResponse();

        if (response == null) {
            log.warn("El modelo no devolvio metadatos de respuesta");
            return new ModelResponse("", null, null, null, null, null);
        }

        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata == null ? null : metadata.getUsage();
        var result = response.getResult();
        String answer = result == null || result.getOutput() == null ? "" : result.getOutput().getText();
        String finishReason = result == null || result.getMetadata() == null
                ? null
                : result.getMetadata().getFinishReason();
        String model = metadata == null || metadata.getModel() == null || metadata.getModel().isBlank()
                ? null
                : metadata.getModel();

        if (answer == null || answer.isBlank()) {
            log.warn("El modelo devolvio una respuesta vacia");
        }

        return new ModelResponse(
                answer == null ? "" : answer,
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(),
                usage == null ? null : usage.getTotalTokens(),
                model,
                finishReason);
    }

    private boolean hasSamplingOptions(ChatRequest request) {
        return request.getTemperature() != null
                || request.getTopP() != null
                || request.getTopK() != null
                || request.getNumPredict() != null
                || request.getSeed() != null;
    }
}
