package com.example.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ClassificationRequest extends SamplingParameters {
    @NotBlank(message = "text must be provided")
    private String text;
    @Pattern(regexp = "(?s).*\\S.*", message = "rol must not be blank")
    private String rol;
    @Pattern(regexp = "(?s).*\\S.*", message = "dominio must not be blank")
    private String dominio;
    @Pattern(regexp = "(?s).*\\S.*", message = "idioma must not be blank")
    private String idioma;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getDominio() { return dominio; }
    public void setDominio(String dominio) { this.dominio = dominio; }
    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }
}
