package com.ticketbooking.system.dto;

import java.time.LocalDateTime;

public record AvailabilityResponse(
        Long eventId,
        String name,
        LocalDateTime eventDate,
        String location,
        Integer totalSeats,
        long heldSeats,
        long bookedSeats,
        long availableSeats
) {
}
