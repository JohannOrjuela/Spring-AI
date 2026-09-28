package com.example.chat.impl;

import com.example.chat.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
    public String generateResponse(String question, String sessionId) {
        log.debug("Enviando prompt al modelo. sessionId={} longitud={}", sessionId, question.length());

        String answer = chatClient.prompt()
                .user(question)
                .call()
                .content();

        if (answer == null || answer.isBlank()) {
            // El ChatController traduce una respuesta en blanco a HTTP 502 + status "error".
            log.warn("El modelo devolvio una respuesta vacia. sessionId={}", sessionId);
            return "";
        }

        log.debug("Respuesta recibida. sessionId={} longitud={}", sessionId, answer.length());
        return answer;
    }
}