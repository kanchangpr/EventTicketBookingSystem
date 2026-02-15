package com.ticketbooking.system.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank String name,
        @NotNull @Future LocalDateTime eventDate,
        @NotBlank String location,
        @NotNull @Min(1) Integer totalSeats
) {
}
