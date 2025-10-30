package com.example.order.filter;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Custom filter to add request tracing information to all HTTP requests.
 * This filter adds a unique request ID and trace information to MDC for logging.
 */
@Component
@Order(1)
public class RequestTracingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestTracingFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String SPAN_ID_HEADER = "X-Span-ID";

    private final Tracer tracer;

    public RequestTracingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();

        try {
            // Generate or extract request ID
            String requestId = extractOrGenerateRequestId(httpRequest);
            MDC.put("requestId", requestId);

            // Add trace context to MDC
            if (tracer.currentSpan() != null) {
                String traceId = tracer.currentSpan().context().traceId();
                String spanId = tracer.currentSpan().context().spanId();

                MDC.put("traceId", traceId);
                MDC.put("spanId", spanId);

                // Add trace IDs to response headers
                httpResponse.setHeader(TRACE_ID_HEADER, traceId);
                httpResponse.setHeader(SPAN_ID_HEADER, spanId);
            }

            // Add request ID to response header
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

            // Add additional context
            MDC.put("method", httpRequest.getMethod());
            MDC.put("uri", httpRequest.getRequestURI());
            MDC.put("remoteAddr", getClientIpAddress(httpRequest));

            // Log request start
            log.info("Request started: {} {} from {}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    getClientIpAddress(httpRequest));

            // Continue with the filter chain
            chain.doFilter(request, response);

            // Calculate duration
            long duration = System.currentTimeMillis() - startTime;

            // Log request completion
            log.info("Request completed: {} {} - Status: {} - Duration: {}ms",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    httpResponse.getStatus(),
                    duration);

            // Add duration to span if available
            if (tracer.currentSpan() != null) {
                tracer.currentSpan().tag("http.duration_ms", String.valueOf(duration));
                tracer.currentSpan().tag("http.status_code", String.valueOf(httpResponse.getStatus()));
            }

        } catch (Exception e) {
            log.error("Error in RequestTracingFilter", e);
            throw e;
        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }

    /**
     * Extract request ID from header or generate a new one
     */
    private String extractOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }

    /**
     * Get client IP address, considering proxy headers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("RequestTracingFilter initialized");
    }

    @Override
    public void destroy() {
        log.info("RequestTracingFilter destroyed");
    }
}

