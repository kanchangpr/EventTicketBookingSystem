package com.ticketbooking.system.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmBookingRequest(@NotBlank String holdId) {
}
