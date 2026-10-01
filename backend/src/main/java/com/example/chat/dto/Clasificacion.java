package com.example.chat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Clasificacion(
        @JsonProperty(required = true) @NotBlank String categoria,
        @JsonProperty(required = true) @NotNull @Min(0) @Max(100) Integer confianza,
        @JsonProperty(required = true) @NotBlank String justificacion) {
}
