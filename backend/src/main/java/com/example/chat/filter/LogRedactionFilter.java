package com.example.chat.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Simple log redaction filter placeholder. Replace patterns for secrets/PII.
 */
@Component
public class LogRedactionFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(LogRedactionFilter.class);
    private static final Pattern SECRET_PATTERN = Pattern.compile("(?i)(api_key|token|password)\"?:?\\s*\\\"?\\w+\\\"?");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            log.debug("Request: method={} path={}", req.getMethod(), req.getRequestURI());
        }
        chain.doFilter(request, response);
    }
}
