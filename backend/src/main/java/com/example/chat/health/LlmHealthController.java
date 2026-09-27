package com.example.chat.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class LlmHealthController {

    private static final Logger log = LoggerFactory.getLogger(LlmHealthController.class);

    private final String modelBaseUrl;
    private final RestTemplate rest = new RestTemplate();

    public LlmHealthController(@Value("${spring.ai.ollama.base-url:http://localhost:11434}") String modelBaseUrl) {
        this.modelBaseUrl = modelBaseUrl;
    }

    @GetMapping("/llm")
    public ResponseEntity<?> llm() {
        try {
            String endpoint = modelBaseUrl.endsWith("/") ? modelBaseUrl + "api/tags" : modelBaseUrl + "/api/tags";
            Map resp = rest.getForObject(endpoint, Map.class);
            return ResponseEntity.ok(Map.of("ok", true, "endpoint", endpoint, "result", resp));
        } catch (Exception e) {
            log.error("LLM health check failed", e);
            return ResponseEntity.status(502).body(Map.of("ok", false, "error", e.getMessage()));
        }
    }
}
