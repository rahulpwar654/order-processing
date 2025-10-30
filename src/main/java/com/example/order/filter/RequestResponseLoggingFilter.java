package com.example.order.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter to log detailed request and response information for debugging.
 * This includes request body, response body, and headers.
 */
@Component
@Order(2)
public class RequestResponseLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    private static final int MAX_PAYLOAD_LENGTH = 1000; // Max characters to log

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip logging for actuator endpoints
        if (httpRequest.getRequestURI().startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        // Wrap request and response to enable reading body multiple times
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        try {
            // Log request details
            logRequestDetails(wrappedRequest);

            // Continue with filter chain
            chain.doFilter(wrappedRequest, wrappedResponse);

            // Log response details
            logResponseDetails(wrappedRequest, wrappedResponse);

        } finally {
            // Copy response body to actual response
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequestDetails(ContentCachingRequestWrapper request) {
        StringBuilder requestLog = new StringBuilder();
        requestLog.append("\n========== REQUEST ==========\n");
        requestLog.append("Method: ").append(request.getMethod()).append("\n");
        requestLog.append("URI: ").append(request.getRequestURI());

        if (request.getQueryString() != null) {
            requestLog.append("?").append(request.getQueryString());
        }
        requestLog.append("\n");

        // Log headers
        requestLog.append("Headers:\n");
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // Skip sensitive headers
            if (!isSensitiveHeader(headerName)) {
                requestLog.append("  ").append(headerName).append(": ")
                          .append(request.getHeader(headerName)).append("\n");
            }
        }

        // Log request body for POST, PUT, PATCH
        if (hasBody(request.getMethod())) {
            String payload = getRequestPayload(request);
            if (!payload.isEmpty()) {
                requestLog.append("Body: ").append(truncate(payload)).append("\n");
            }
        }

        requestLog.append("============================");
        log.debug(requestLog.toString());
    }

    private void logResponseDetails(ContentCachingRequestWrapper request,
                                   ContentCachingResponseWrapper response) {
        StringBuilder responseLog = new StringBuilder();
        responseLog.append("\n========== RESPONSE ==========\n");
        responseLog.append("Status: ").append(response.getStatus()).append("\n");
        responseLog.append("Method: ").append(request.getMethod()).append(" ");
        responseLog.append("URI: ").append(request.getRequestURI()).append("\n");

        // Log response headers
        responseLog.append("Headers:\n");
        for (String headerName : response.getHeaderNames()) {
            if (!isSensitiveHeader(headerName)) {
                responseLog.append("  ").append(headerName).append(": ")
                          .append(response.getHeader(headerName)).append("\n");
            }
        }

        // Log response body
        String responseBody = getResponsePayload(response);
        if (!responseBody.isEmpty()) {
            responseLog.append("Body: ").append(truncate(responseBody)).append("\n");
        }

        responseLog.append("=============================");
        log.debug(responseLog.toString());
    }

    private String getRequestPayload(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                return new String(buf, 0, Math.min(buf.length, MAX_PAYLOAD_LENGTH),
                                StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "[Unable to parse request body]";
            }
        }
        return "";
    }

    private String getResponsePayload(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0) {
            try {
                return new String(buf, 0, Math.min(buf.length, MAX_PAYLOAD_LENGTH),
                                StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "[Unable to parse response body]";
            }
        }
        return "";
    }

    private boolean hasBody(String method) {
        return "POST".equalsIgnoreCase(method) ||
               "PUT".equalsIgnoreCase(method) ||
               "PATCH".equalsIgnoreCase(method);
    }

    private boolean isSensitiveHeader(String headerName) {
        String lowerCaseName = headerName.toLowerCase();
        return lowerCaseName.contains("authorization") ||
               lowerCaseName.contains("password") ||
               lowerCaseName.contains("token") ||
               lowerCaseName.contains("secret") ||
               lowerCaseName.contains("api-key");
    }

    private String truncate(String str) {
        if (str.length() > MAX_PAYLOAD_LENGTH) {
            return str.substring(0, MAX_PAYLOAD_LENGTH) + "... [truncated]";
        }
        return str;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("RequestResponseLoggingFilter initialized");
    }

    @Override
    public void destroy() {
        log.info("RequestResponseLoggingFilter destroyed");
    }
}

