package com.ticketbooking.system.repository;

import com.ticketbooking.system.entity.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :eventId")
    Optional<Event> findByIdForUpdate(@Param("eventId") Long eventId);

    Optional<Event> findByNameIgnoreCaseAndEventDateAndLocationIgnoreCase(String name,
                                                                           LocalDateTime eventDate,
                                                                           String location);

    boolean existsByNameIgnoreCaseAndEventDateAndLocationIgnoreCaseAndIdNot(String name,
                                                                             LocalDateTime eventDate,
                                                                             String location,
                                                                             Long id);
}
