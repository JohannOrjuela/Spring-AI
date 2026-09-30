package com.example.chat.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ChatRequest {
    private String sessionId;

    @NotBlank(message = "question must be provided")
    private String question;

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

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
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
