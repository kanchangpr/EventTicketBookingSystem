package com.ticketbooking.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventTicketBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventTicketBookingSystemApplication.class, args);
    }
}
