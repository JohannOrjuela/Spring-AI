package com.example.chat.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Success-aware boundary around process-local chat memory.
 *
 * <p>The provider invocation remains inside the exact-session gate so a later
 * accepted call observes the preceding successful turn. System instructions
 * and request options never enter this service.</p>
 */
@Service
public class ConversationMemoryService {
    public static final int MAX_NON_SYSTEM_MESSAGES = 20;

    private final ChatMemory chatMemory;
    private final ConcurrentHashMap<String, ReentrantLock> sessionGates = new ConcurrentHashMap<>();

    public ConversationMemoryService(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    public <T> T execute(
            String sessionId,
            String renderedUserMessage,
            Function<List<Message>, TurnResult<T>> providerInvocation) {
        Objects.requireNonNull(renderedUserMessage, "renderedUserMessage must not be null");
        Objects.requireNonNull(providerInvocation, "providerInvocation must not be null");

        UserMessage currentUser = new UserMessage(renderedUserMessage);
        if (sessionId == null) {
            return requireResult(providerInvocation.apply(List.of(currentUser))).value();
        }
        if (sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }

        ReentrantLock gate = sessionGates.computeIfAbsent(sessionId, ignored -> new ReentrantLock(true));
        gate.lock();
        try {
            List<Message> providerMessages = boundedPrompt(chatMemory.get(sessionId), currentUser);
            TurnResult<T> result = requireResult(providerInvocation.apply(providerMessages));
            if (result.assistantText() != null && !result.assistantText().isBlank()) {
                chatMemory.add(sessionId, List.of(currentUser, new AssistantMessage(result.assistantText())));
            }
            return result.value();
        } finally {
            gate.unlock();
        }
    }

    List<Message> retainedMessages(String sessionId) {
        return List.copyOf(chatMemory.get(sessionId));
    }

    private List<Message> boundedPrompt(List<Message> retained, UserMessage currentUser) {
        int retainedLimit = MAX_NON_SYSTEM_MESSAGES - 1;
        int start = Math.max(0, retained.size() - retainedLimit);
        List<Message> messages = new ArrayList<>(Math.min(MAX_NON_SYSTEM_MESSAGES, retained.size() + 1));
        messages.addAll(retained.subList(start, retained.size()));
        messages.add(currentUser);
        return List.copyOf(messages);
    }

    private <T> TurnResult<T> requireResult(TurnResult<T> result) {
        return Objects.requireNonNull(result, "providerInvocation returned null");
    }

    public record TurnResult<T>(T value, String assistantText) {
    }
}
