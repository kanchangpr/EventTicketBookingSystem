package com.ticketbooking.system.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketbooking.system.dto.EventRequest;
import com.ticketbooking.system.dto.EventResponse;
import com.ticketbooking.system.exception.ValidationException;
import com.ticketbooking.system.service.EventService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);
    private final EventService eventService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public EventController(EventService eventService, ObjectMapper objectMapper, Validator validator) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Object create(@RequestBody JsonNode requestBody) {
        if (requestBody.isArray()) {
            List<EventRequest> requests = new ArrayList<>();
            for (JsonNode node : requestBody) {
                EventRequest request = objectMapper.convertValue(node, EventRequest.class);
                validate(request);
                requests.add(request);
            }
            log.info("Create events request received count={}", requests.size());
            return eventService.createAll(requests);
        }

        if (requestBody.isObject()) {
            EventRequest request = objectMapper.convertValue(requestBody, EventRequest.class);
            validate(request);
            log.info("Create event request received name={} date={}", request.name(), request.eventDate());
            return eventService.create(request);
        }

        throw new ValidationException("Request body must be either an event object or an array of event objects");
    }

    @GetMapping
    public List<EventResponse> list() {
        log.info("List events request received");
        return eventService.list();
    }

    @GetMapping("/{eventId}")
    public EventResponse get(@PathVariable Long eventId) {
        log.info("Get event request received eventId={}", eventId);
        return eventService.get(eventId);
    }

    @PutMapping("/{eventId}")
    public EventResponse update(@RequestBody EventRequest request, @PathVariable Long eventId) {
        validate(request);
        log.info("Update event request received eventId={} name={} date={}", eventId, request.name(), request.eventDate());
        return eventService.update(eventId, request);
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long eventId) {
        log.info("Delete event request received eventId={}", eventId);
        eventService.delete(eventId);
    }

    private void validate(EventRequest request) {
        Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed");
            throw new ValidationException("Validation failed: " + message);
        }
    }
}
