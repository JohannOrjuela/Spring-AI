package com.example.chat;

import com.example.chat.dto.ChatResponse;
import com.example.chat.dto.ClassificationRequest;
import com.example.chat.dto.ClassificationResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/v1")
public class ClassificationController {
    private static final Logger log = LoggerFactory.getLogger(ClassificationController.class);
    private final ModelService modelService;
    public ClassificationController(ModelService modelService) { this.modelService = modelService; }

    @PostMapping("/classifications")
    public ResponseEntity<ClassificationResponse> classify(@Valid @RequestBody ClassificationRequest request, BindingResult binding) {
        if (binding.hasErrors()) {
            List<ChatResponse.ValidationError> errors = binding.getFieldErrors().stream()
                    .map(error -> new ChatResponse.ValidationError(error.getField(), error.getDefaultMessage())).toList();
            log.warn("Validation failed for classification request fields={}", errors.stream().map(ChatResponse.ValidationError::field).toList());
            return ResponseEntity.badRequest().body(new ClassificationResponse(UUID.randomUUID().toString(), null, "Request validation failed", 0, "error",
                    null, null, null, null, null, null, errors));
        }
        long start = System.nanoTime();
        ModelService.ClassificationModelResponse result = modelService.classify(request);
        long elapsed = Math.max(0, (System.nanoTime() - start) / 1_000_000);
        Double rate = result.completionTokens() == null || elapsed <= 0 ? null
                : Math.round((result.completionTokens() / (elapsed / 1000.0)) * 100.0) / 100.0;
        var response = new ClassificationResponse(UUID.randomUUID().toString(), result.clasificacion(), null, elapsed, "ok",
                result.promptTokens(), result.completionTokens(), result.totalTokens(), result.model(), result.finishReason(), rate, null);
        log.info("Classification completed elapsedMs={} status=ok model={} promptTokens={} completionTokens={} totalTokens={} tokensPerSecond={}",
                elapsed, result.model(), result.promptTokens(), result.completionTokens(), result.totalTokens(), rate);
        return ResponseEntity.ok(response);
    }
}
