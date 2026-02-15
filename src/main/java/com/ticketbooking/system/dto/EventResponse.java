package com.ticketbooking.system.dto;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String name,
        LocalDateTime eventDate,
        String location,
        Integer totalSeats
) {
}
