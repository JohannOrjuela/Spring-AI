package com.example.chat.prompt;

import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public final class PromptTemplateRegistry {
    public static final String DEFAULT_TEMPLATE = "conciso";
    private static final Map<String, Resource> TEMPLATES = Map.of(
            "conciso", new ClassPathResource("prompts/conciso.st"),
            "tutor", new ClassPathResource("prompts/tutor.st"),
            "extractor", new ClassPathResource("prompts/extractor.st"));
    private static final Resource USER_TEMPLATE = new ClassPathResource("prompts/user.st");
    private final StTemplateRenderer renderer = StTemplateRenderer.builder()
            .startDelimiterToken('<').endDelimiterToken('>').build();

    public String renderSystem(String templateId, Map<String, Object> variables) {
        checkVariables(variables);
        String selected = templateId == null ? DEFAULT_TEMPLATE : templateId;
        Resource resource = TEMPLATES.get(selected);
        if (resource == null) throw new UnknownTemplateException();
        return renderer.apply(read(resource), variables);
    }

    public String renderUser(Map<String, Object> variables) {
        checkVariables(variables);
        return renderer.apply(read(USER_TEMPLATE), variables);
    }

    private String read(Resource resource) {
        try { return resource.getContentAsString(StandardCharsets.UTF_8); }
        catch (IOException ex) { throw new IllegalStateException("Prompt resource unavailable", ex); }
    }

    private void checkVariables(Map<String, Object> variables) {
        if (!java.util.Set.of("rol", "dominio", "idioma", "question").containsAll(variables.keySet()))
            throw new IllegalArgumentException("Unsupported template variable");
    }
}
