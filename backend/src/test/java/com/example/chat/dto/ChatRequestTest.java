package com.example.chat.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRequestTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void acceptsMinimumMaximumAndRepresentativeValues() {
        ChatRequest minimum = validRequest();
        minimum.setTemperature(0.0);
        minimum.setTopP(0.0);
        minimum.setTopK(0);
        minimum.setNumPredict(1);
        assertThat(VALIDATOR.validate(minimum)).isEmpty();

        ChatRequest maximum = validRequest();
        maximum.setTemperature(2.0);
        maximum.setTopP(1.0);
        maximum.setTopK(200);
        maximum.setNumPredict(2048);
        assertThat(VALIDATOR.validate(maximum)).isEmpty();

        ChatRequest representative = validRequest();
        representative.setTemperature(0.7);
        representative.setTopP(0.9);
        representative.setTopK(40);
        representative.setNumPredict(512);
        assertThat(VALIDATOR.validate(representative)).isEmpty();
    }

    @Test
    void rejectsValuesBelowAboveAndRepresentativeInvalidValues() {
        List<InvalidCase> cases = List.of(
                new InvalidCase("temperature", request -> request.setTemperature(-0.01)),
                new InvalidCase("temperature", request -> request.setTemperature(2.01)),
                new InvalidCase("temperature", request -> request.setTemperature(17.0)),
                new InvalidCase("topP", request -> request.setTopP(-0.01)),
                new InvalidCase("topP", request -> request.setTopP(1.01)),
                new InvalidCase("topP", request -> request.setTopP(9.0)),
                new InvalidCase("topK", request -> request.setTopK(-1)),
                new InvalidCase("topK", request -> request.setTopK(201)),
                new InvalidCase("topK", request -> request.setTopK(500)),
                new InvalidCase("numPredict", request -> request.setNumPredict(0)),
                new InvalidCase("numPredict", request -> request.setNumPredict(2049)),
                new InvalidCase("numPredict", request -> request.setNumPredict(10000)));

        for (InvalidCase invalidCase : cases) {
            ChatRequest request = validRequest();
            invalidCase.mutation.accept(request);
            assertThat(VALIDATOR.validate(request))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains(invalidCase.field);
        }
    }

    @Test
    void acceptsSeedAcrossEntireIntegerRange() {
        for (int seed : List.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE)) {
            ChatRequest request = validRequest();
            request.setSeed(seed);
            assertThat(VALIDATOR.validate(request)).isEmpty();
        }
    }

    @Test
    void mixedInputReportsOnlyInvalidFields() {
        ChatRequest request = validRequest();
        request.setTemperature(0.3);
        request.setTopP(4.0);
        request.setTopK(40);
        request.setNumPredict(0);

        assertThat(VALIDATOR.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("topP", "numPredict");
    }

    private static ChatRequest validRequest() {
        ChatRequest request = new ChatRequest();
        request.setQuestion("question");
        return request;
    }

    @Test void contextStaysNullAndOnlyBlankValuesAreRejected() {
        var mapper=tools.jackson.databind.json.JsonMapper.builder().build();
        for(String json:List.of("{\"question\":\"x\"}","{\"question\":\"x\",\"templateId\":null,\"rol\":null,\"dominio\":null,\"idioma\":null}")) {
            var req=mapper.readValue(json,ChatRequest.class);
            assertThat(req.getTemplateId()).isNull();assertThat(req.getRol()).isNull();
            assertThat(req.getDominio()).isNull();assertThat(req.getIdioma()).isNull();
        }
        var req=validRequest();req.setRol(" \n");req.setDominio("");req.setIdioma(" ");req.setTemplateId(" ");
        assertThat(VALIDATOR.validate(req)).hasSize(4);
        req.setRol("long\n".repeat(2000));req.setDominio(null);req.setIdioma(null);req.setTemplateId(null);
        assertThat(VALIDATOR.validate(req)).isEmpty();
    }

    @Test
    void sessionIdIsNullableExactAndNonblankWhenPresent() {
        for (String accepted : List.of("A", "a", " A ", "session-006")) {
            ChatRequest request = validRequest();
            request.setSessionId(accepted);
            assertThat(VALIDATOR.validate(request)).isEmpty();
            assertThat(request.getSessionId()).isEqualTo(accepted);
        }

        ChatRequest omitted = validRequest();
        assertThat(VALIDATOR.validate(omitted)).isEmpty();
        omitted.setSessionId(null);
        assertThat(VALIDATOR.validate(omitted)).isEmpty();

        for (String rejected : List.of("", " ", "\t\r\n")) {
            ChatRequest request = validRequest();
            request.setSessionId(rejected);
            assertThat(VALIDATOR.validate(request))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("sessionId");
        }
    }
    private record InvalidCase(String field, Consumer<ChatRequest> mutation) {}
}
