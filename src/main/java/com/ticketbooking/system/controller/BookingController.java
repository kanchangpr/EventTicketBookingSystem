package com.ticketbooking.system.controller;

import com.ticketbooking.system.dto.*;
import com.ticketbooking.system.service.BookingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/events/{eventId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse holdSeats(@PathVariable Long eventId, @Valid @RequestBody HoldSeatsRequest request) {
        log.info("Hold seats request received eventId={} userId={}", eventId, request.userId());
        return bookingService.holdSeats(eventId, request);
    }

    @PostMapping("/bookings/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse confirmBooking(@Valid @RequestBody ConfirmBookingRequest request) {
        log.info("Confirm booking request received holdId={}", request.holdId());
        return bookingService.confirmBooking(request);
    }

    @GetMapping({"/bookings", "/bookings/{bookingId}"})
    public Object getBookings(@PathVariable(required = false) Long bookingId) {
        if (bookingId == null) {
            log.info("List bookings request received");
            return bookingService.listBookings();
        }
        log.info("View booking request received bookingId={}", bookingId);
        return bookingService.viewBooking(bookingId);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public BookingResponse cancelBooking(@PathVariable Long bookingId) {
        log.info("Cancel booking request received bookingId={}", bookingId);
        return bookingService.cancelBooking(bookingId);
    }

    @GetMapping("/holds")
    public Object listHolds(@RequestParam(required = false) Long eventId,
                            @RequestParam(required = false) String userId) {
        log.info("List holds request received eventId={} userId={}", eventId, userId);
        return bookingService.listHolds(eventId, userId);
    }

    @GetMapping({"/events/availability", "/events/{eventId}/availability"})
    public Object availability(@PathVariable(required = false) Long eventId) {
        if (eventId == null) {
            return bookingService.availabilityAll();
        }
        return bookingService.availability(eventId);
    }
}
