package com.example.chat.exception;

import com.example.chat.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ChatResponse> handleHttpClient(HttpClientErrorException ex) {
        log.error("HTTP client error when calling model", ex);
        ChatResponse resp = new ChatResponse(null, "", 0, "error");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(resp);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ChatResponse> handleRuntime(RuntimeException ex) {
        log.error("Runtime exception in chat service", ex);
        ChatResponse resp = new ChatResponse(null, "", 0, "error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }
}
