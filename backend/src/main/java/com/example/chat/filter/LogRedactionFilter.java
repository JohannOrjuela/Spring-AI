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
import java.util.Set;

/**
 * Logs only known route labels. Bodies, query strings and arbitrary paths are never logged.
 */
@Component
public class LogRedactionFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(LogRedactionFilter.class);
    private static final Set<String> ROUTES = Set.of("/api/v1/chat", "/api/v1/classifications", "/health/llm");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            String route = ROUTES.contains(req.getRequestURI()) ? req.getRequestURI() : "other";
            log.debug("Request: method={} route={}", req.getMethod(), route);
        }
        chain.doFilter(request, response);
    }
}
