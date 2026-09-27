package com.example.chat.dto;

public class ChatResponse {
    private String requestId;
    private String answer;
    private long elapsedMs;
    private String status;

    public ChatResponse() {}

    public ChatResponse(String requestId, String answer, long elapsedMs, String status) {
        this.requestId = requestId;
        this.answer = answer;
        this.elapsedMs = elapsedMs;
        this.status = status;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
