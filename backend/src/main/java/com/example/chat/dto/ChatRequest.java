package com.example.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ChatRequest extends SamplingParameters {
    private String sessionId;

    @NotBlank(message = "question must be provided")
    private String question;

    @Pattern(regexp = "conciso|tutor|extractor", message = "templateId must be conciso, tutor or extractor")
    private String templateId;
    @Pattern(regexp = "(?s).*\\S.*", message = "rol must not be blank")
    private String rol;
    @Pattern(regexp = "(?s).*\\S.*", message = "dominio must not be blank")
    private String dominio;
    @Pattern(regexp = "(?s).*\\S.*", message = "idioma must not be blank")
    private String idioma;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getDominio() { return dominio; }
    public void setDominio(String dominio) { this.dominio = dominio; }
    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }
}
