package com.ticketbooking.system.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component("customRequestContextFilter")
public class RequestContextFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String TRACE_ID = "X-Trace-Id";
    public static final String SPAN_ID = "X-Span-Id";

    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    private final ObjectMapper objectMapper;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public RequestContextFilter(ObjectMapper objectMapper,
                                HandlerExceptionResolver handlerExceptionResolver) {
        this.objectMapper = objectMapper;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();

        String correlationId = nullableHeader(request, CORRELATION_ID, UUID.randomUUID().toString());
        String traceId = nullableHeader(request, TRACE_ID, UUID.randomUUID().toString().replace("-", ""));
        String spanId = nullableHeader(request, SPAN_ID, Long.toHexString(System.nanoTime()));

        try {
            MDC.put("correlationId", correlationId);
            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
            MDC.put("method", request.getMethod());
            MDC.put("path", request.getRequestURI());

            response.setHeader(CORRELATION_ID, correlationId);
            response.setHeader(TRACE_ID, traceId);
            response.setHeader(SPAN_ID, spanId);

            try {
                filterChain.doFilter(request, response);
            } catch (Exception ex) {
                handlerExceptionResolver.resolveException(request, response, null, ex);
            }
        } catch (Exception ex) {
            handlerExceptionResolver.resolveException(request, response, null, ex);
        } finally {
            logStructured(request, response, start, correlationId, traceId, spanId);
            MDC.clear();
        }
    }

    private String nullableHeader(HttpServletRequest request, String headerName, String fallback) {
        String value = request.getHeader(headerName);
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }


    private void logStructured(HttpServletRequest request,
                               HttpServletResponse response,
                               long start,
                               String correlationId,
                               String traceId,
                               String spanId) {
        long durationMs = System.currentTimeMillis() - start;
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("timestamp", Instant.now().toString());
        event.put("level", "INFO");
        event.put("correlationId", correlationId);
        event.put("traceId", traceId);
        event.put("spanId", spanId);
        event.put("method", request.getMethod());
        event.put("path", request.getRequestURI());
        event.put("status", response.getStatus());
        event.put("durationMs", durationMs);

        try {
            log.info(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.info("{} {} status={} durationMs={} correlationId={} traceId={} spanId={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs,
                    correlationId, traceId, spanId);
        }
    }
}
