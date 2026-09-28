package com.example.chat.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {
    private String sessionId;

    @NotBlank(message = "question must be provided")
    private String question;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
