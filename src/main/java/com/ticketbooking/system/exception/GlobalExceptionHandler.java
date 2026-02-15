package com.ticketbooking.system.exception;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MeterRegistry meterRegistry;

    public GlobalExceptionHandler(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return buildError(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(HoldExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleHoldExpired(HoldExpiredException ex) {
        return buildError(HttpStatus.GONE, "HOLD_EXPIRED", ex.getMessage());
    }

    @ExceptionHandler({ValidationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Map<String, Object>> handleValidation(Exception ex) {
        String message;
        if (ex instanceof MethodArgumentNotValidException manv) {
            StringBuilder builder = new StringBuilder("Validation failed: ");
            for (FieldError error : manv.getBindingResult().getFieldErrors()) {
                builder.append(error.getField()).append(" ").append(error.getDefaultMessage()).append("; ");
            }
            message = builder.toString();
        } else {
            message = ex.getMessage();
        }
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedRequest(HttpMessageNotReadableException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed JSON or invalid request format");
    }

    @ExceptionHandler(PreconditionRequiredException.class)
    public ResponseEntity<Map<String, Object>> handlePrecondition(PreconditionRequiredException ex) {
        return buildError(HttpStatus.PRECONDITION_REQUIRED, "PRECONDITION_REQUIRED", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error");
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String code, String message) {
        if (meterRegistry != null) {
            Counter.builder("ticketbooking.api.errors")
                    .tag("status", Integer.toString(status.value()))
                    .tag("code", code)
                    .register(meterRegistry)
                    .increment();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("code", code);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
