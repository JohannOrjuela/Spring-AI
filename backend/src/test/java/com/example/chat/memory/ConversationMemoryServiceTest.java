package com.example.chat.memory;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationMemoryServiceTest {

    @Test
    void sameSessionReceivesSuccessfulTurnsInChronologicalOrder() {
        ConversationMemoryService service = service();
        service.execute("session", "user-1", messages -> turn("result-1", "assistant-1"));
        AtomicReference<List<Message>> observed = new AtomicReference<>();

        String result = service.execute("session", "user-2", messages -> {
            observed.set(messages);
            return turn("result-2", "assistant-2");
        });

        assertThat(result).isEqualTo("result-2");
        assertThat(texts(observed.get())).containsExactly("user-1", "assistant-1", "user-2");
        assertThat(texts(service.retainedMessages("session")))
                .containsExactly("user-1", "assistant-1", "user-2", "assistant-2");
    }

    @Test
    void omittedAndNullSessionsStayStateless() {
        ConversationMemoryService service = service();
        List<List<String>> prompts = new ArrayList<>();

        service.execute(null, "first", messages -> {
            prompts.add(texts(messages));
            return turn("first-result", "first-answer");
        });
        service.execute(null, "second", messages -> {
            prompts.add(texts(messages));
            return turn("second-result", "second-answer");
        });

        assertThat(prompts).containsExactly(List.of("first"), List.of("second"));
    }

    @Test
    void failedAndBlankProviderResultsAreNeverRetained() {
        ConversationMemoryService service = service();

        assertThatThrownBy(() -> service.execute("session", "failed-user", messages -> {
            throw new IllegalStateException("provider failed");
        })).isInstanceOf(IllegalStateException.class);
        service.execute("session", "blank-user", messages -> turn("blank-result", "  "));
        AtomicReference<List<Message>> observed = new AtomicReference<>();
        service.execute("session", "successful-user", messages -> {
            observed.set(messages);
            return turn("ok", "successful-answer");
        });

        assertThat(texts(observed.get())).containsExactly("successful-user");
        assertThat(texts(service.retainedMessages("session")))
                .containsExactly("successful-user", "successful-answer");
    }

    @Test
    void promptWindowCountsCurrentUserAndEvictsStrictlyOldestMessage() {
        ConversationMemoryService service = service();
        for (int turn = 1; turn <= 10; turn++) {
            int current = turn;
            service.execute("session", "user-" + current,
                    messages -> turn("result-" + current, "assistant-" + current));
        }
        AtomicReference<List<Message>> observed = new AtomicReference<>();

        service.execute("session", "user-11", messages -> {
            observed.set(messages);
            return turn("result-11", "assistant-11");
        });

        assertThat(observed.get()).hasSize(20);
        assertThat(texts(observed.get()).getFirst()).isEqualTo("assistant-1");
        assertThat(texts(observed.get()).getLast()).isEqualTo("user-11");
        assertThat(service.retainedMessages("session")).hasSize(20);
    }

    @Test
    void promptBoundariesAtNineteenTwentyAndTwentyOneMessagesAreExact() {
        for (int retainedCount : List.of(18, 19, 20)) {
            ChatMemory memory = memory();
            List<Message> retained = new ArrayList<>();
            for (int index = 1; index <= retainedCount; index++) {
                retained.add(new UserMessage("retained-" + index));
            }
            memory.add("session", retained);
            ConversationMemoryService service = new ConversationMemoryService(memory);
            AtomicReference<List<Message>> observed = new AtomicReference<>();

            service.execute("session", "current", messages -> {
                observed.set(messages);
                return turn("result", "answer");
            });

            int unboundedCount = retainedCount + 1;
            assertThat(observed.get()).hasSize(Math.min(20, unboundedCount));
            assertThat(texts(observed.get()).getLast()).isEqualTo("current");
            assertThat(texts(observed.get()).getFirst())
                    .isEqualTo(unboundedCount > 20 ? "retained-2" : "retained-1");
        }
    }

    @Test
    void exactIdentifiersRemainIsolatedWithoutCaseOrWhitespaceNormalization() {
        ConversationMemoryService service = service();
        for (String id : List.of("A", "B", "Case", "case", " Case ")) {
            service.execute(id, "user-" + id, messages -> turn("result", "assistant-" + id));
        }

        for (String id : List.of("A", "B", "Case", "case", " Case ")) {
            AtomicReference<List<Message>> observed = new AtomicReference<>();
            service.execute(id, "follow-up-" + id, messages -> {
                observed.set(messages);
                return turn("result", "answer");
            });
            assertThat(texts(observed.get()))
                    .containsExactly("user-" + id, "assistant-" + id, "follow-up-" + id);
        }
    }

    @Test
    void sameSessionCallsSerializeAndLaterCallSeesPriorSuccessfulTurn() throws Exception {
        ConversationMemoryService service = service();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        AtomicBoolean secondEntered = new AtomicBoolean();
        AtomicReference<List<Message>> secondPrompt = new AtomicReference<>();

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> service.execute("session", "first", messages -> {
                firstEntered.countDown();
                await(releaseFirst);
                return turn("first-result", "first-answer");
            }));
            assertThat(firstEntered.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                secondStarted.countDown();
                return service.execute("session", "second", messages -> {
                    secondEntered.set(true);
                    secondPrompt.set(messages);
                    return turn("second-result", "second-answer");
                });
            });

            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondEntered).isFalse();
            releaseFirst.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("first-result");
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("second-result");
        }

        assertThat(texts(secondPrompt.get())).containsExactly("first", "first-answer", "second");
    }

    @Test
    void differentSessionsCanInvokeProviderConcurrently() throws Exception {
        ConversationMemoryService service = service();
        CountDownLatch bothEntered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> service.execute("A", "first", messages -> {
                bothEntered.countDown();
                await(release);
                return turn("first-result", "first-answer");
            }));
            var second = executor.submit(() -> service.execute("B", "second", messages -> {
                bothEntered.countDown();
                await(release);
                return turn("second-result", "second-answer");
            }));

            assertThat(bothEntered.await(5, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("first-result");
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("second-result");
        }
    }

    @Test
    void newApplicationMemoryHasNoPriorSessions() {
        AtomicReference<List<Message>> observed = new AtomicReference<>();
        try (AnnotationConfigApplicationContext firstContext = applicationContext()) {
            firstContext.getBean(ConversationMemoryService.class)
                    .execute("session", "before-restart", messages -> turn("result", "answer"));
        }
        try (AnnotationConfigApplicationContext restartedContext = applicationContext()) {
            restartedContext.getBean(ConversationMemoryService.class)
                    .execute("session", "after-restart", messages -> {
                        observed.set(messages);
                        return turn("result", "answer");
                    });
        }

        assertThat(texts(observed.get())).containsExactly("after-restart");
    }

    private static AnnotationConfigApplicationContext applicationContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(ConversationMemoryConfiguration.class, ConversationMemoryService.class);
        context.refresh();
        return context;
    }

    private static ConversationMemoryService service() {
        return new ConversationMemoryService(memory());
    }

    private static ChatMemory memory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(ConversationMemoryService.MAX_NON_SYSTEM_MESSAGES)
                .build();
    }

    private static ConversationMemoryService.TurnResult<String> turn(String value, String answer) {
        return new ConversationMemoryService.TurnResult<>(value, answer);
    }

    private static List<String> texts(List<Message> messages) {
        return messages.stream().map(Message::getText).toList();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test latch");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
