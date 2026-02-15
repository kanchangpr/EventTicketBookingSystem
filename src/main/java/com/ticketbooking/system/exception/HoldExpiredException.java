package com.ticketbooking.system.exception;

public class HoldExpiredException extends RuntimeException {
    public HoldExpiredException(String message) {
        super(message);
    }
}
