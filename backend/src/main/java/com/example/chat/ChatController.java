package com.example.chat;

import com.example.chat.dto.ChatRequest;
import com.example.chat.dto.ChatResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/v1")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ModelService modelService;

    public ChatController(ModelService modelService) {
        this.modelService = modelService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest req, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<ChatResponse.ValidationError> errors = bindingResult.getFieldErrors().stream()
                    .map(error -> new ChatResponse.ValidationError(error.getField(), error.getDefaultMessage()))
                    .toList();
            List<String> validationCodes = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ":" + error.getCode())
                    .toList();
            log.warn("Validation failed for chat request fields={}", validationCodes);
            ChatResponse bad = new ChatResponse(UUID.randomUUID().toString(), "Request validation failed", 0, "error",
                    null, null, null, null, null, null);
            bad.setErrors(errors);
            return ResponseEntity.badRequest().body(bad);
        }

        long start = System.nanoTime();
        ModelService.ModelResponse modelResponse = modelService.generateResponse(req);
        long elapsed = Math.max(0, (System.nanoTime() - start) / 1_000_000);

        String answer = modelResponse == null || modelResponse.answer() == null ? "" : modelResponse.answer();
        String status = (answer == null || answer.isBlank()) ? "error" : "ok";
        Double tokensPerSecond = calculateTokensPerSecond(modelResponse, elapsed);
        ChatResponse resp = new ChatResponse(
                UUID.randomUUID().toString(),
                answer,
                elapsed,
                status,
                modelResponse == null ? null : modelResponse.promptTokens(),
                modelResponse == null ? null : modelResponse.completionTokens(),
                modelResponse == null ? null : modelResponse.totalTokens(),
                modelResponse == null ? null : modelResponse.model(),
                modelResponse == null ? null : modelResponse.finishReason(),
                tokensPerSecond);

        log.info("Chat response completed elapsedMs={} status={} model={} promptTokens={} completionTokens={} totalTokens={} tokensPerSecond={}",
                elapsed, status, resp.getModel(), resp.getPromptTokens(), resp.getCompletionTokens(), resp.getTotalTokens(),
                resp.getTokensPerSecond());
        return status.equals("ok") ? ResponseEntity.ok(resp) : ResponseEntity.status(502).body(resp);
    }

    private Double calculateTokensPerSecond(ModelService.ModelResponse response, long elapsedMs) {
        if (response == null || response.completionTokens() == null || elapsedMs <= 0) {
            return null;
        }
        double tokensPerSecond = response.completionTokens() / (elapsedMs / 1000.0);
        return Math.round(tokensPerSecond * 100.0) / 100.0;
    }
}
