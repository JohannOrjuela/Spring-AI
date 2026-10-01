package com.example.chat;

import com.example.chat.dto.*;
import com.example.chat.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClassificationControllerTest {
    private ModelService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;
    @BeforeEach void setup() {
        service=mock(ModelService.class);validator=new LocalValidatorFactoryBean();validator.afterPropertiesSet();
        mvc=MockMvcBuilders.standaloneSetup(new ClassificationController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
        when(service.classify(any())).thenReturn(new ModelService.ClassificationModelResponse(new Clasificacion("support",90,"reason"),10,5,16,"gemma3:4b","stop"));
    }
    @AfterEach void close(){validator.close();}
    @Test void returnsTypedClassificationAndCompleteMetrics() throws Exception {
        mvc.perform(post("/api/v1/classifications").contentType("application/json")
            .content("{\"text\":\"hello\",\"temperature\":0,\"topK\":0,\"seed\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.requestId",not(emptyOrNullString())))
            .andExpect(jsonPath("$.classification.categoria").value("support"))
            .andExpect(jsonPath("$.classification.confianza").value(90))
            .andExpect(jsonPath("$.classification.justificacion").value("reason"))
            .andExpect(jsonPath("$.answer").value(nullValue())).andExpect(jsonPath("$.promptTokens").value(10))
            .andExpect(jsonPath("$.completionTokens").value(5)).andExpect(jsonPath("$.totalTokens").value(16))
            .andExpect(jsonPath("$.model").value("gemma3:4b")).andExpect(jsonPath("$.finishReason").value("stop"))
            .andExpect(jsonPath("$.elapsedMs").isNumber());
        var capture=org.mockito.ArgumentCaptor.forClass(ClassificationRequest.class);verify(service).classify(capture.capture());
        assertThat(capture.getValue().getTemperature()).isZero();assertThat(capture.getValue().getTopK()).isZero();
    }
    @Test void rejectsUnknownPropertiesInvalidTypesAndInvalidInputBeforeModel() throws Exception {
        for(String property:new String[]{"templateId","systemPrompt","promptPath","resource","template","question","sessionId","anything"}) {
            mvc.perform(post("/api/v1/classifications").contentType("application/json").content("{\"text\":\"x\",\""+property+"\":\"secret\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.requestId",not(emptyOrNullString())))
                .andExpect(jsonPath("$.classification").value(nullValue())).andExpect(jsonPath("$.answer").value("Request validation failed"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
        }
        for(String json:new String[]{"{}","{\"text\":\" \"}","{\"text\":\"x\",\"rol\":\" \"}","{\"text\":\"x\",\"temperature\":17}","{\"text\":\"x\",\"numPredict\":0}","{\"text\":\"x\",\"seed\":2147483648}"})
            mvc.perform(post("/api/v1/classifications").contentType("application/json").content(json)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void modelFailureHasSafeMessageIdAndAllNullableMetrics() throws Exception {
        when(service.classify(any())).thenThrow(new ModelOperationException(System.nanoTime()-1_000_000));
        var result=mvc.perform(post("/api/v1/classifications").contentType("application/json").content("{\"text\":\"private\"}"))
            .andExpect(status().isBadGateway()).andExpect(jsonPath("$.requestId",not(emptyOrNullString())))
            .andExpect(jsonPath("$.answer").value("Model operation failed")).andExpect(jsonPath("$.classification").value(nullValue()));
        for(String field:new String[]{"promptTokens","completionTokens","totalTokens","model","finishReason","tokensPerSecond"})
            result.andExpect(jsonPath("$."+field).value(nullValue()));
    }
}
