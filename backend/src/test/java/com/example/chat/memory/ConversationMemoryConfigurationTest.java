package com.example.chat.memory;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMemoryConfigurationTest {

    @Test
    void createsOneProcessLocalTwentyMessageWindow() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                ConversationMemoryConfiguration.class)) {
            ChatMemory memory = context.getBean(ChatMemory.class);
            List<UserMessage> messages = new ArrayList<>();
            for (int index = 1; index <= 21; index++) {
                messages.add(new UserMessage("message-" + index));
            }

            memory.add("session", List.copyOf(messages));

            assertThat(context.getBeansOfType(ChatMemory.class)).hasSize(1);
            assertThat(memory.get("session")).hasSize(20);
            assertThat(memory.get("session").getFirst().getText()).isEqualTo("message-2");
        }
    }
}
