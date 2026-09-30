package com.example.chat.dto;

import java.util.List;

public class ChatResponse {
    private String requestId;
    private String answer;
    private long elapsedMs;
    private String status;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String model;
    private String finishReason;
    private Double tokensPerSecond;
    private List<ValidationError> errors;

    public record ValidationError(String field, String message) {}

    public ChatResponse() {}

    public ChatResponse(String requestId, String answer, long elapsedMs, String status) {
        this(requestId, answer, elapsedMs, status, null, null, null, null, null, null);
    }

    public ChatResponse(
            String requestId,
            String answer,
            long elapsedMs,
            String status,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            String model,
            String finishReason,
            Double tokensPerSecond) {
        this.requestId = requestId;
        this.answer = answer;
        this.elapsedMs = elapsedMs;
        this.status = status;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.model = model;
        this.finishReason = finishReason;
        this.tokensPerSecond = tokensPerSecond;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    public Double getTokensPerSecond() { return tokensPerSecond; }
    public void setTokensPerSecond(Double tokensPerSecond) { this.tokensPerSecond = tokensPerSecond; }
    public List<ValidationError> getErrors() { return errors; }
    public void setErrors(List<ValidationError> errors) { this.errors = errors; }
}
