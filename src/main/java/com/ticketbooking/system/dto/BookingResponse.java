package com.ticketbooking.system.dto;

import com.ticketbooking.system.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long bookingId,
        Long eventId,
        String userId,
        BookingStatus status,
        LocalDateTime createdAt,
        LocalDateTime canceledAt,
        List<Integer> seats
) {
}
