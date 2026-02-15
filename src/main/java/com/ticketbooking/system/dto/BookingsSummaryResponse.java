package com.ticketbooking.system.dto;

import java.util.List;

public record BookingsSummaryResponse(
        List<BookingWithHoldsResponse> bookings,
        List<HoldResponse> holds
) {
}
