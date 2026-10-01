package com.example.chat.impl;

import com.example.chat.ModelService;
import com.example.chat.dto.ChatRequest;
import com.example.chat.dto.Clasificacion;
import com.example.chat.dto.ClassificationRequest;
import com.example.chat.dto.SamplingParameters;
import com.example.chat.prompt.PromptTemplateRegistry;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import com.example.chat.exception.ModelOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

@Service
public class SpringAiModelService implements ModelService {
    private static final Logger log = LoggerFactory.getLogger(SpringAiModelService.class);
    private static final JsonMapper OUTPUT_MAPPER = JsonMapper.builder().build();
    private static final String DEFAULT_ROLE = "asistente";
    private static final String DEFAULT_DOMAIN = "general";
    private static final String DEFAULT_LANGUAGE = "el idioma del usuario";
    private final ChatClient chatClient;
    private final PromptTemplateRegistry templates;
    private final Validator validator;

    public SpringAiModelService(ChatClient.Builder builder, PromptTemplateRegistry templates, Validator validator) {
        this.chatClient = builder.build();
        this.templates = templates;
        this.validator = validator;
    }

    @Override
    public ModelResponse generateResponse(ChatRequest request) {
        Map<String, Object> variables = variables(request.getRol(), request.getDominio(), request.getIdioma(), request.getQuestion());
        var prompt = withOptions(chatClient.prompt().system(templates.renderSystem(request.getTemplateId(), variables))
                .user(templates.renderUser(variables)).templateRenderer((text, params) -> text), request);
        long start = System.nanoTime();
        ProviderData data;
        try { data = providerData(prompt.call().chatResponse()); }
        catch (RuntimeException ex) { throw new ModelOperationException(start); }
        return new ModelResponse(data.answer(), data.promptTokens(), data.completionTokens(), data.totalTokens(), data.model(), data.finishReason());
    }

    @Override
    public ClassificationModelResponse classify(ClassificationRequest request) {
        Map<String, Object> variables = variables(request.getRol(), request.getDominio(), request.getIdioma(), request.getText());
        var prompt = withOptions(chatClient.prompt().system(templates.renderSystem("extractor", variables))
                .user(templates.renderUser(variables)).templateRenderer((text, params) -> text), request);
        long start = System.nanoTime();
        try {
            ResponseEntity<org.springframework.ai.chat.model.ChatResponse, Clasificacion> converted = prompt.call()
                    .responseEntity(Clasificacion.class, spec -> spec.useProviderStructuredOutput().validateSchema());
            validateFinalJson(converted.response());
            Clasificacion entity = converted.entity();
            if (entity == null) throw new IllegalStateException("Structured model response was empty");
            var violations = validator.validate(entity);
            if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
            ProviderData data = providerData(converted.response());
            return new ClassificationModelResponse(entity, data.promptTokens(), data.completionTokens(), data.totalTokens(), data.model(), data.finishReason());
        } catch (RuntimeException ex) { throw new ModelOperationException(start); }
    }

    private ChatClient.ChatClientRequestSpec withOptions(ChatClient.ChatClientRequestSpec prompt, SamplingParameters request) {
        if (!hasSamplingOptions(request)) return prompt;
        OllamaChatOptions.Builder options = OllamaChatOptions.builder();
        if (request.getTemperature() != null) options.temperature(request.getTemperature());
        if (request.getTopP() != null) options.topP(request.getTopP());
        if (request.getTopK() != null) options.topK(request.getTopK());
        if (request.getNumPredict() != null) options.numPredict(request.getNumPredict());
        if (request.getSeed() != null) options.seed(request.getSeed());
        return prompt.options(options);
    }

    /** Schema correction can return its last response; never accept Jackson's numeric coercion. */
    private void validateFinalJson(org.springframework.ai.chat.model.ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null)
            throw new IllegalStateException("Missing classification response");
        var json = OUTPUT_MAPPER.readTree(response.getResult().getOutput().getText());
        if (json == null || !json.isObject() || json.size() != 3
                || !json.hasNonNull("categoria") || !json.get("categoria").isString()
                || !json.hasNonNull("justificacion") || !json.get("justificacion").isString()
                || !json.hasNonNull("confianza") || !json.get("confianza").isIntegralNumber()
                || !json.get("confianza").canConvertToInt())
            throw new IllegalStateException("Invalid classification JSON contract");
    }

    private boolean hasSamplingOptions(SamplingParameters request) {
        return request.getTemperature() != null || request.getTopP() != null || request.getTopK() != null
                || request.getNumPredict() != null || request.getSeed() != null;
    }

    private Map<String, Object> variables(String role, String domain, String language, String question) {
        return Map.of("rol", role == null ? DEFAULT_ROLE : role, "dominio", domain == null ? DEFAULT_DOMAIN : domain,
                "idioma", language == null ? DEFAULT_LANGUAGE : language, "question", question);
    }

    private ProviderData providerData(org.springframework.ai.chat.model.ChatResponse response) {
        if (response == null) return new ProviderData("", null, null, null, null, null);
        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata == null ? null : metadata.getUsage();
        var result = response.getResult();
        String answer = result == null || result.getOutput() == null ? "" : result.getOutput().getText();
        String finishReason = result == null || result.getMetadata() == null ? null : result.getMetadata().getFinishReason();
        String model = metadata == null || metadata.getModel() == null || metadata.getModel().isBlank() ? null : metadata.getModel();
        if (answer == null || answer.isBlank()) log.warn("Model returned an empty response");
        return new ProviderData(answer == null ? "" : answer, usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(), usage == null ? null : usage.getTotalTokens(), model, finishReason);
    }

    private record ProviderData(String answer, Integer promptTokens, Integer completionTokens, Integer totalTokens,
                                String model, String finishReason) {}
}
