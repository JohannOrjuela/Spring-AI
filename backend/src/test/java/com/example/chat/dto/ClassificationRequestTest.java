package com.example.chat.dto;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class ClassificationRequestTest {
    private final JsonMapper mapper=JsonMapper.builder().build();
    @Test void defaultsStayNullAndSamplingIsFlatAndZeroAware() {
        var dto=mapper.readValue("{\"text\":\"hello\",\"temperature\":0.0,\"topK\":0,\"seed\":-2147483648}",ClassificationRequest.class);
        assertThat(dto.getRol()).isNull();assertThat(dto.getDominio()).isNull();assertThat(dto.getIdioma()).isNull();
        assertThat(dto.getTemperature()).isZero();assertThat(dto.getTopK()).isZero();assertThat(dto.getSeed()).isEqualTo(Integer.MIN_VALUE);
        assertThat(dto.getTopP()).isNull();assertThat(dto.getNumPredict()).isNull();
        assertThat(mapper.writeValueAsString(dto)).doesNotContain("samplingParameters");
    }
    @Test void validatesTextContextAndAllSamplingLimits() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();var req=new ClassificationRequest();
            assertThat(validator.validate(req)).extracting(v->v.getPropertyPath().toString()).contains("text");
            req.setText("text");req.setRol(" \n");req.setDominio(" ");req.setIdioma("");
            req.setTemperature(17.0);req.setTopP(1.1);req.setTopK(201);req.setNumPredict(2049);
            assertThat(validator.validate(req)).extracting(v->v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("rol","dominio","idioma","temperature","topP","topK","numPredict");
            for(String json:new String[]{"{\"text\":\"x\",\"rol\":null,\"temperature\":0,\"topP\":0,\"topK\":0,\"numPredict\":1,\"seed\":-2147483648}",
                "{\"text\":\"x\",\"temperature\":2,\"topP\":1,\"topK\":200,\"numPredict\":2048,\"seed\":2147483647}"}) {
                req=mapper.readValue(json,ClassificationRequest.class);req.setRol("long\n".repeat(2000));
                assertThat(validator.validate(req)).isEmpty();
            }
        }
    }
}
