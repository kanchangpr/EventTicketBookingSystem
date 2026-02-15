package com.ticketbooking.system.config;

import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class DownstreamPropagationConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.additionalInterceptors((request, body, execution) -> {
            copyIfPresent(request, ApiRequestContextFilter.CORRELATION_ID, "correlationId");
            copyIfPresent(request, ApiRequestContextFilter.TRACE_ID, "traceId");
            copyIfPresent(request, ApiRequestContextFilter.SPAN_ID, "spanId");
            return execution.execute(request, body);
        }).build();
    }

    private void copyIfPresent(org.springframework.http.HttpRequest request, String headerName, String mdcKey) {
        String value = MDC.get(mdcKey);
        if (value != null && !value.isBlank()) {
            request.getHeaders().set(headerName, value);
        }
    }
}
