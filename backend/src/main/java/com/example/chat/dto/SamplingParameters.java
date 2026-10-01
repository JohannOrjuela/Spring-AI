package com.example.chat.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class SamplingParameters {
    /** Reject unknown fields even when the surrounding mapper uses relaxed defaults. */
    @JsonAnySetter
    public final void rejectUnknownProperty(String name, Object value) {
        throw new IllegalArgumentException("Unknown request property");
    }

    @DecimalMin(value = "0.0", message = "temperature must be greater than or equal to 0.0")
    @DecimalMax(value = "2.0", message = "temperature must be less than or equal to 2.0")
    private Double temperature;
    @DecimalMin(value = "0.0", message = "topP must be greater than or equal to 0.0")
    @DecimalMax(value = "1.0", message = "topP must be less than or equal to 1.0")
    private Double topP;
    @Min(value = 0, message = "topK must be greater than or equal to 0")
    @Max(value = 200, message = "topK must be less than or equal to 200")
    private Integer topK;
    @Min(value = 1, message = "numPredict must be greater than or equal to 1")
    @Max(value = 2048, message = "numPredict must be less than or equal to 2048")
    private Integer numPredict;
    private Integer seed;

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public Integer getNumPredict() { return numPredict; }
    public void setNumPredict(Integer numPredict) { this.numPredict = numPredict; }
    public Integer getSeed() { return seed; }
    public void setSeed(Integer seed) { this.seed = seed; }
}
