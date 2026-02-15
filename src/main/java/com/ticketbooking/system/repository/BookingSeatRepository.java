package com.ticketbooking.system.repository;

import com.ticketbooking.system.entity.BookingSeat;
import com.ticketbooking.system.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    @Query("""
            select bs.seatNumber from BookingSeat bs
            where bs.booking.event.id = :eventId and bs.booking.status = :status
            """)
    List<Integer> findSeatNumbersForBookingStatus(@Param("eventId") Long eventId,
                                                  @Param("status") BookingStatus status);

    @Query("""
            select count(bs) from BookingSeat bs
            where bs.booking.event.id = :eventId and bs.booking.status = :status
            """)
    long countForBookingStatus(@Param("eventId") Long eventId,
                               @Param("status") BookingStatus status);
}
