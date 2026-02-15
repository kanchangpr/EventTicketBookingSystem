package com.ticketbooking.system.repository;

import com.ticketbooking.system.entity.SeatHoldItem;
import com.ticketbooking.system.enums.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatHoldItemRepository extends JpaRepository<SeatHoldItem, Long> {

    @Query("""
            select shi.seatNumber from SeatHoldItem shi
            where shi.hold.event.id = :eventId
              and shi.hold.status = :status
              and shi.hold.expiresAt > :now
            """)
    List<Integer> findSeatNumbersForActiveHolds(@Param("eventId") Long eventId,
                                                @Param("status") HoldStatus status,
                                                @Param("now") LocalDateTime now);

    @Query("""
            select count(shi) from SeatHoldItem shi
            where shi.hold.event.id = :eventId
              and shi.hold.status = :status
              and shi.hold.expiresAt > :now
            """)
    long countForActiveHolds(@Param("eventId") Long eventId,
                             @Param("status") HoldStatus status,
                             @Param("now") LocalDateTime now);
}
