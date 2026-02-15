package com.ticketbooking.system.repository;

import com.ticketbooking.system.entity.SeatHold;
import com.ticketbooking.system.enums.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatHoldRepository extends JpaRepository<SeatHold, String> {
    List<SeatHold> findByStatusAndExpiresAtBefore(HoldStatus status, LocalDateTime cutoff);

    @Query("""
            select h.id from SeatHold h
            where h.event.id = :eventId
              and h.userId = :userId
              and h.status = :status
            """)
    List<String> findHoldIdsForEventAndUser(@Param("eventId") Long eventId,
                                            @Param("userId") String userId,
                                            @Param("status") HoldStatus status);

    List<SeatHold> findByStatus(HoldStatus status);

    List<SeatHold> findByStatusAndEventId(HoldStatus status, Long eventId);

    List<SeatHold> findByStatusAndUserId(HoldStatus status, String userId);

    List<SeatHold> findByStatusAndEventIdAndUserId(HoldStatus status, Long eventId, String userId);
}
