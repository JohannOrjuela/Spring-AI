package com.example.chat;

import com.example.chat.exception.GlobalExceptionHandler;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.HttpClientErrorException;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import org.slf4j.LoggerFactory;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest {

    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;
    private StubModelService modelService;
    private ListAppender<ILoggingEvent> controllerLogAppender;
    private ListAppender<ILoggingEvent> serviceLogAppender;

    @BeforeEach
    void setUp() {
        modelService = new StubModelService();
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(modelService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
            controllerLogAppender = attachAppender(ChatController.class);
            serviceLogAppender = attachAppender(com.example.chat.impl.SpringAiModelService.class);
    }

    @AfterEach
    void tearDown() {
        validator.close();
        detachAppender(ChatController.class, controllerLogAppender);
        detachAppender(com.example.chat.impl.SpringAiModelService.class, serviceLogAppender);
    }

    @Test
    void returnsPreservedAndMetricFieldsForSuccessfulChat() throws Exception {
        modelService.response = new ModelService.ModelResponse(
                "answer", 12, 8, 21, "llama3", "stop");

        mockMvc.perform(post("/api/v1/chat")
                        .contentType("application/json")
                        .content("{\"question\":\"question\",\"sessionId\":\"session\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.answer").value("answer"))
                .andExpect(jsonPath("$.elapsedMs", notNullValue()))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.promptTokens").value(12))
                .andExpect(jsonPath("$.completionTokens").value(8))
                .andExpect(jsonPath("$.totalTokens").value(21))
                .andExpect(jsonPath("$.model").value("llama3"))
                .andExpect(jsonPath("$.finishReason").value("stop"))
                .andExpect(jsonPath("$.tokensPerSecond", nullValue()));
    }

    @Test
    void preservesCompleteNullMetricShapeForModelFailure() throws Exception {
        modelService.failure = new HttpClientErrorException(HttpStatus.BAD_GATEWAY);

        mockMvc.perform(post("/api/v1/chat")
                        .contentType("application/json")
                        .content("{\"question\":\"question\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.promptTokens").value(nullValue()))
                .andExpect(jsonPath("$.completionTokens").value(nullValue()))
                .andExpect(jsonPath("$.totalTokens").value(nullValue()))
                .andExpect(jsonPath("$.model").value(nullValue()))
                .andExpect(jsonPath("$.finishReason").value(nullValue()))
                .andExpect(jsonPath("$.tokensPerSecond").value(nullValue()));
    }

    @Test
    void preservesCompleteNullMetricShapeForRuntimeFailure() throws Exception {
        modelService.failure = new IllegalStateException("model failure");

        mockMvc.perform(post("/api/v1/chat")
                        .contentType("application/json")
                        .content("{\"question\":\"question\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.promptTokens").value(nullValue()))
                .andExpect(jsonPath("$.completionTokens").value(nullValue()))
                .andExpect(jsonPath("$.totalTokens").value(nullValue()))
                .andExpect(jsonPath("$.model").value(nullValue()))
                .andExpect(jsonPath("$.finishReason").value(nullValue()))
                .andExpect(jsonPath("$.tokensPerSecond").value(nullValue()));
    }

    @Test
    void rejectsBlankQuestionWithClientError() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType("application/json")
                        .content("{\"question\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

            @Test
            void preservesLegacyFieldsForExistingClients() throws Exception {
            modelService.response = new ModelService.ModelResponse(
                "answer", 12, 8, 20, "llama3", "stop");

            mockMvc.perform(post("/api/v1/chat")
                    .contentType("application/json")
                    .content("{\"question\":\"question\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.answer").value("answer"))
                .andExpect(jsonPath("$.elapsedMs", notNullValue()))
                .andExpect(jsonPath("$.status").value("ok"));
            }

            @Test
            void doesNotLogQuestionOrAnswerContent() throws Exception {
            modelService.response = new ModelService.ModelResponse(
                "private-answer", 12, 8, 20, "llama3", "stop");

            mockMvc.perform(post("/api/v1/chat")
                    .contentType("application/json")
                    .content("{\"question\":\"private-question\"}"))
                .andExpect(status().isOk());

            String controllerLogs = controllerLogAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + right);
            String serviceLogs = serviceLogAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + right);
            assertThat(controllerLogs).doesNotContain("private-question", "private-answer");
            assertThat(serviceLogs).doesNotContain("private-question", "private-answer");
            }

            private static ListAppender<ILoggingEvent> attachAppender(Class<?> source) {
            Logger logger = (Logger) LoggerFactory.getLogger(source);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            return appender;
            }

            private static void detachAppender(Class<?> source, ListAppender<ILoggingEvent> appender) {
            Logger logger = (Logger) LoggerFactory.getLogger(source);
            logger.detachAppender(appender);
            appender.stop();
            }

    private static final class StubModelService implements ModelService {
        private ModelResponse response = new ModelResponse("answer", null, null, null, null, null);
        private RuntimeException failure;

        @Override
        public ModelResponse generateResponse(String question, String sessionId) {
            if (failure != null) {
                throw failure;
            }
            return response;
        }
    }
}
