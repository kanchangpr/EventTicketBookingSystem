package com.ticketbooking.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record HoldSeatsRequest(
        @NotBlank String userId,
        @NotEmpty List<Integer> seatNumbers
) {
}
