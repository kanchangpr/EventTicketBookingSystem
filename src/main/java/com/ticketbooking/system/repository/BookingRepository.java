package com.ticketbooking.system.repository;

import com.ticketbooking.system.entity.Booking;
import com.ticketbooking.system.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByEventIdAndUserIdAndStatus(Long eventId, String userId, BookingStatus status);

    boolean existsByHoldIdAndUserIdAndStatus(String holdId, String userId, BookingStatus status);
}
