package com.example.chat.exception;

import com.example.chat.dto.ChatResponse;
import com.example.chat.dto.ClassificationResponse;
import com.example.chat.prompt.UnknownTemplateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import java.util.List;
import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({UnknownTemplateException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Rejected API request exceptionType={}", ex.getClass().getSimpleName());
        String field = ex instanceof UnknownTemplateException ? "templateId" : "request";
        String detail = ex instanceof UnknownTemplateException ? "templateId must be conciso, tutor or extractor"
                : "JSON must contain only documented fields with the expected types";
        return error(request, HttpStatus.BAD_REQUEST, "Request validation failed", 0,
                List.of(new ChatResponse.ValidationError(field, detail)));
    }
    @ExceptionHandler({HttpClientErrorException.class, ConstraintViolationException.class, ModelOperationException.class})
    public ResponseEntity<?> handleProvider(Exception ex, HttpServletRequest request) {
        log.error("Model operation failed exceptionType={}", ex.getClass().getSimpleName());
        return error(request, HttpStatus.BAD_GATEWAY, "Model operation failed",
                ex instanceof ModelOperationException failure ? failure.getElapsedMs() : 0, null);
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        log.error("Application operation failed exceptionType={}", ex.getClass().getSimpleName());
        return error(request, HttpStatus.INTERNAL_SERVER_ERROR, "Operation failed", 0, null);
    }
    private ResponseEntity<?> error(HttpServletRequest request, HttpStatus status, String message, long elapsedMs,
                                    List<ChatResponse.ValidationError> errors) {
        String requestId = UUID.randomUUID().toString();
        if (request.getRequestURI().endsWith("/classifications")) {
            return ResponseEntity.status(status).body(new ClassificationResponse(requestId, null, message, elapsedMs, "error",
                    null, null, null, null, null, null, errors));
        }
        ChatResponse body = new ChatResponse(requestId, message, elapsedMs, "error", null, null, null, null, null, null);
        body.setErrors(errors);
        return ResponseEntity.status(status).body(body);
    }
}
