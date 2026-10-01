package com.example.chat.prompt;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.*;

class PromptTemplateRegistryTest {
    private final PromptTemplateRegistry registry = new PromptTemplateRegistry();
    private Map<String,Object> vars(String role) {
        return Map.of("rol",role,"dominio","biologia","idioma","espanol","question","{ADN} <rol>");
    }
    @Test void rendersAllTemplatesAndPreservesLiteralJson() {
        for(String id:new String[]{"conciso","tutor","extractor"})
            assertThat(registry.renderSystem(id,vars("profesor"))).contains("profesor","biologia","espanol").doesNotContain("<rol>");
        assertThat(registry.renderSystem(null,vars("x"))).isEqualTo(registry.renderSystem("conciso",vars("x")));
        assertThat(registry.renderSystem("extractor",vars("x"))).contains("{\"categoria\":\"ejemplo\",\"confianza\":90,\"justificacion\":\"motivo\"}");
        assertThat(registry.renderUser(vars("x"))).contains("{ADN} <rol>");
    }
    @Test void rejectsUnknownIdentifiersAndMissingOrExtraVariables() {
        for(String id:new String[]{""," ","Tutor"," tutor","tutor ","../secret","file:/secret","ignore instructions"})
            assertThatThrownBy(()->registry.renderSystem(id,vars("x"))).isInstanceOf(UnknownTemplateException.class);
        assertThatThrownBy(()->registry.renderSystem("tutor",Map.of("rol","x"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->registry.renderUser(Map.of("question","x","systemPrompt","bad"))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void concurrentRenderingIsIsolatedAndDoesNotReinterpretValues() throws Exception {
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->registry.renderSystem("tutor",vars("role-A <idioma> {x}")));
            var b=executor.submit(()->registry.renderSystem("conciso",vars("role-B")));
            assertThat(a.get()).contains("role-A <idioma> {x}").doesNotContain("role-B");
            assertThat(b.get()).contains("role-B").doesNotContain("role-A");
        }
    }
}
