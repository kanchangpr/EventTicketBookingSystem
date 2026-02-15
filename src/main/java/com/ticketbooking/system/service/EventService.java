package com.ticketbooking.system.service;

import com.ticketbooking.system.dto.EventRequest;
import com.ticketbooking.system.dto.EventResponse;
import com.ticketbooking.system.entity.Event;
import com.ticketbooking.system.exception.ConflictException;
import com.ticketbooking.system.exception.NotFoundException;
import com.ticketbooking.system.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public EventResponse create(EventRequest request) {
        EventRequest normalized = normalize(request);

        return eventRepository.findByNameIgnoreCaseAndEventDateAndLocationIgnoreCase(
                        normalized.name(), normalized.eventDate(), normalized.location())
                .map(existing -> {
                    log.info("Event create deduplicated eventId={} name={}", existing.getId(), existing.getName());
                    return toResponse(existing);
                })
                .orElseGet(() -> {
                    Event event = toEntity(normalized);
                    Event saved = eventRepository.save(event);
                    log.info("Event created eventId={} name={}", saved.getId(), saved.getName());
                    return toResponse(saved);
                });
    }

    public List<EventResponse> createAll(List<EventRequest> requests) {
        log.info("Creating events batch count={}", requests.size());
        return requests.stream().map(this::create).toList();
    }

    public List<EventResponse> list() {
        log.info("Listing events");
        return eventRepository.findAll().stream().map(this::toResponse).toList();
    }

    public EventResponse get(Long id) {
        log.info("Getting event eventId={}", id);
        return toResponse(getEntity(id));
    }

    public EventResponse update(Long id, EventRequest request) {
        log.info("Updating event eventId={}", id);
        Event event = getEntity(id);
        EventRequest normalized = normalize(request);

        if (eventRepository.existsByNameIgnoreCaseAndEventDateAndLocationIgnoreCaseAndIdNot(
                normalized.name(), normalized.eventDate(), normalized.location(), id)) {
            throw new ConflictException("Another event already exists with same name, date, and location");
        }

        event.setName(normalized.name());
        event.setEventDate(normalized.eventDate());
        event.setLocation(normalized.location());
        event.setTotalSeats(normalized.totalSeats());
        Event saved = eventRepository.save(event);
        log.info("Event updated eventId={} name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    public void delete(Long id) {
        Event event = getEntity(id);
        eventRepository.delete(event);
        log.info("Event deleted eventId={}", id);
    }

    public Event getEntity(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: " + id));
    }

    private EventRequest normalize(EventRequest request) {
        return new EventRequest(
                request.name().trim(),
                request.eventDate(),
                request.location().trim(),
                request.totalSeats()
        );
    }

    private Event toEntity(EventRequest request) {
        Event event = new Event();
        event.setName(request.name());
        event.setEventDate(request.eventDate());
        event.setLocation(request.location());
        event.setTotalSeats(request.totalSeats());
        return event;
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getTotalSeats()
        );
    }
}
