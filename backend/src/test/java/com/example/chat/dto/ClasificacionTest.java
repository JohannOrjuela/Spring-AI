package com.example.chat.dto;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class ClasificacionTest {
    @Test void schemaRequiresEveryPropertyAndIntegerConfidence() {
        var schema=JsonMapper.builder().build().readTree(new BeanOutputConverter<>(Clasificacion.class).getJsonSchema());
        assertThat(schema.get("required").toString()).contains("categoria","confianza","justificacion");
        assertThat(schema.at("/properties/confianza/type").asText()).isEqualTo("integer");
    }
    @Test void validatesRequiredConfidenceAndStrings() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            for(Integer confidence:new Integer[]{null,-1,101})
                assertThat(validator.validate(new Clasificacion("cat",confidence,"reason"))).isNotEmpty();
            for(int confidence:new int[]{0,100})
                assertThat(validator.validate(new Clasificacion("cat",confidence,"reason"))).isEmpty();
            assertThat(validator.validate(new Clasificacion(" ",50,""))).hasSize(2);
        }
        var converter=new BeanOutputConverter<>(Clasificacion.class);
        assertThatThrownBy(()->converter.convert("{\"categoria\":\"cat\",\"justificacion\":\"r\"}")).isInstanceOf(RuntimeException.class);
    }
}
