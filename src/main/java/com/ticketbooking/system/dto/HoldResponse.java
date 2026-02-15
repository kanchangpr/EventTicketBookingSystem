package com.ticketbooking.system.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HoldResponse(
        String holdId,
        Long eventId,
        String userId,
        LocalDateTime expiresAt,
        List<Integer> seats
) {
}
