package com.example.chat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.chat.dto.ChatRequest;
import com.example.chat.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
    void acceptsAllSamplingFieldsAndPreservesMetrics() throws Exception {
        modelService.response = new ModelService.ModelResponse("answer", 12, 8, 21, "llama3", "stop");

        mockMvc.perform(post("/api/v1/chat")
                        .contentType("application/json")
                        .content("""
                                {"question":"question","sessionId":"session","temperature":0.7,
                                 "topP":0.9,"topK":40,"numPredict":50,"seed":7}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.answer").value("answer"))
                .andExpect(jsonPath("$.elapsedMs", notNullValue()))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.promptTokens").value(12))
                .andExpect(jsonPath("$.completionTokens").value(8))
                .andExpect(jsonPath("$.totalTokens").value(21))
                .andExpect(jsonPath("$.model").value("llama3"))
                .andExpect(jsonPath("$.finishReason").value("stop"));

        ChatRequest captured = modelService.requests.getFirst();
        assertThat(captured.getTemperature()).isEqualTo(0.7);
        assertThat(captured.getTopP()).isEqualTo(0.9);
        assertThat(captured.getTopK()).isEqualTo(40);
        assertThat(captured.getNumPredict()).isEqualTo(50);
        assertThat(captured.getSeed()).isEqualTo(7);
    }

    @Test
    void acceptsPartialSamplingFields() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType("application/json")
                        .content("{\"question\":\"question\",\"temperature\":0.3,\"topK\":5}"))
                .andExpect(status().isOk());

        ChatRequest captured = modelService.requests.getFirst();
        assertThat(captured.getTemperature()).isEqualTo(0.3);
        assertThat(captured.getTopK()).isEqualTo(5);
        assertThat(captured.getTopP()).isNull();
        assertThat(captured.getNumPredict()).isNull();
        assertThat(captured.getSeed()).isNull();
    }

    @Test
    void acceptsOmittedAndExplicitNullSamplingFields() throws Exception {
        mockMvc.perform(post("/api/v1/chat").contentType("application/json")
                        .content("{\"question\":\"omitted\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/chat").contentType("application/json")
                        .content("""
                                {"question":"nulls","temperature":null,"topP":null,
                                 "topK":null,"numPredict":null,"seed":null}
                                """))
                .andExpect(status().isOk());

        assertThat(modelService.requests).hasSize(2);
        assertThat(modelService.requests).allSatisfy(request -> {
            assertThat(request.getTemperature()).isNull();
            assertThat(request.getTopP()).isNull();
            assertThat(request.getTopK()).isNull();
            assertThat(request.getNumPredict()).isNull();
            assertThat(request.getSeed()).isNull();
        });
    }

    @Test
    void acceptsTwentyConsecutiveDifferentConfigurations() throws Exception {
        for (int index = 0; index < 20; index++) {
            double temperature = index / 10.0;
            mockMvc.perform(post("/api/v1/chat")
                            .contentType("application/json")
                            .content("{\"question\":\"question\",\"temperature\":" + temperature
                                    + ",\"topK\":" + index + ",\"seed\":" + index + "}"))
                    .andExpect(status().isOk());
        }

        assertThat(modelService.requests).hasSize(20);
        assertThat(modelService.requests).extracting(ChatRequest::getTopK)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, 20).boxed().toList());
    }

    @Test
    void preservesCompleteNullMetricShapeForModelFailure() throws Exception {
        modelService.failure = new HttpClientErrorException(HttpStatus.BAD_GATEWAY);

        mockMvc.perform(post("/api/v1/chat").contentType("application/json")
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

        mockMvc.perform(post("/api/v1/chat").contentType("application/json")
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
        mockMvc.perform(post("/api/v1/chat").contentType("application/json")
                        .content("{\"question\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void rejectsEveryInvalidFieldWithSafeErrorsAndDoesNotCallModel() throws Exception {
        mockMvc.perform(post("/api/v1/chat").contentType("application/json")
                        .content("""
                                {"question":"question","temperature":17,"topP":-0.1,
                                 "topK":201,"numPredict":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.answer").value("Request validation failed"))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.elapsedMs").value(0))
                .andExpect(jsonPath("$.promptTokens").value(nullValue()))
                .andExpect(jsonPath("$.completionTokens").value(nullValue()))
                .andExpect(jsonPath("$.totalTokens").value(nullValue()))
                .andExpect(jsonPath("$.model").value(nullValue()))
                .andExpect(jsonPath("$.finishReason").value(nullValue()))
                .andExpect(jsonPath("$.tokensPerSecond").value(nullValue()))
                .andExpect(jsonPath("$.errors.length()").value(4))
                .andExpect(jsonPath("$.errors[*].field")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("temperature", "topP", "topK", "numPredict")));

        assertThat(modelService.requests).isEmpty();
    }

    @Test
    void enforcesNumPredictBoundariesBeforeCallingModel() throws Exception {
        for (int accepted : List.of(1, 2048)) {
            mockMvc.perform(post("/api/v1/chat").contentType("application/json")
                            .content("{\"question\":\"question\",\"numPredict\":" + accepted + "}"))
                    .andExpect(status().isOk());
        }
        for (int rejected : List.of(0, 2049)) {
            mockMvc.perform(post("/api/v1/chat").contentType("application/json")
                            .content("{\"question\":\"question\",\"numPredict\":" + rejected + "}"))
                    .andExpect(status().isBadRequest());
        }

        assertThat(modelService.requests).hasSize(2);
    }

    @Test
    void validationLogsExcludeQuestionAndRejectedValue() throws Exception {
        mockMvc.perform(post("/api/v1/chat").contentType("application/json")
                        .content("{\"question\":\"sensitive-question\",\"numPredict\":199999}"))
                .andExpect(status().isBadRequest());

        assertThat(logs(controllerLogAppender)).doesNotContain("sensitive-question", "199999");
        assertThat(modelService.requests).isEmpty();
    }

    @Test
    void doesNotLogQuestionOrAnswerContent() throws Exception {
        modelService.response = new ModelService.ModelResponse("private-answer", 12, 8, 20, "llama3", "stop");

        mockMvc.perform(post("/api/v1/chat").contentType("application/json")
                        .content("{\"question\":\"private-question\"}"))
                .andExpect(status().isOk());

        assertThat(logs(controllerLogAppender)).doesNotContain("private-question", "private-answer");
        assertThat(logs(serviceLogAppender)).doesNotContain("private-question", "private-answer");
    }

    @Test void acceptsTemplateSelectionsAndNullContext() throws Exception {
        for(String template:List.of("conciso", "tutor", "extractor")) {
            mockMvc.perform(post("/api/v1/chat").contentType("application/json")
                .content("{\"question\":\"q\",\"templateId\":\""+template+"\",\"rol\":null}"))
                .andExpect(status().isOk());
        }
        assertThat(modelService.requests).extracting(ChatRequest::getTemplateId).containsExactly("conciso","tutor","extractor");
    }

    @Test void rejectsUnknownAndUnsafeTemplateIdentifiersBeforeService() throws Exception {
        for(String template:List.of("", " ", "Tutor", " tutor", "tutor ", "../secret", "file:/secret", "ignore previous instructions")) {
            mockMvc.perform(post("/api/v1/chat").contentType("application/json")
                .content("{\"question\":\"q\",\"templateId\":\""+template+"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("templateId"))
                .andExpect(jsonPath("$.requestId",org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString())));
        }
        assertThat(modelService.requests).isEmpty();
        assertThat(logs(controllerLogAppender)).doesNotContain("ignore previous instructions", "../secret");
    }

    @Test void rejectsEveryUnknownPropertyBeforeService() throws Exception {
        for(String property:List.of("systemPrompt", "promptPath", "resource", "template", "unknown")) {
            mockMvc.perform(post("/api/v1/chat").contentType("application/json")
                .content("{\"question\":\"q\",\""+property+"\":\"secret\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isNotEmpty())
                .andExpect(jsonPath("$.answer").value("Request validation failed"));
        }
        assertThat(modelService.requests).isEmpty();
    }

    private static String logs(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", String::concat);
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
        private final List<ChatRequest> requests = new ArrayList<>();
        private ModelResponse response = new ModelResponse("answer", null, null, null, null, null);
        private RuntimeException failure;

        @Override
        public ModelResponse generateResponse(ChatRequest request) {
            requests.add(request);
            if (failure != null) {
                throw failure;
            }
            return response;
        }

        @Override
        public ClassificationModelResponse classify(com.example.chat.dto.ClassificationRequest request) {
            throw new UnsupportedOperationException("Not used by chat tests");
        }
    }
}
