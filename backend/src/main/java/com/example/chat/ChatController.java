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

import java.time.Instant;
import java.util.UUID;

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
            String err = bindingResult.getAllErrors().toString();
            log.warn("Validation failed for chat request: {}", err);
            ChatResponse bad = new ChatResponse(UUID.randomUUID().toString(), "", 0, "error");
            return ResponseEntity.badRequest().body(bad);
        }

        log.info("Received chat request sessionId={} question={}", req.getSessionId(), req.getQuestion());
        long start = Instant.now().toEpochMilli();
        String answer = modelService.generateResponse(req.getQuestion(), req.getSessionId());
        long elapsed = Instant.now().toEpochMilli() - start;

        String status = (answer == null || answer.isBlank()) ? "error" : "ok";
        ChatResponse resp = new ChatResponse(UUID.randomUUID().toString(), answer == null ? "" : answer, elapsed, status);

        log.info("Chat response for sessionId={} elapsedMs={} status={} response={}", req.getSessionId(), elapsed, status, resp);
        return status.equals("ok") ? ResponseEntity.ok(resp) : ResponseEntity.status(502).body(resp);
    }
}
