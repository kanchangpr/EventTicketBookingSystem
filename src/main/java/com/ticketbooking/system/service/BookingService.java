package com.ticketbooking.system.service;

import com.ticketbooking.system.dto.*;
import com.ticketbooking.system.entity.*;
import com.ticketbooking.system.enums.BookingStatus;
import com.ticketbooking.system.enums.HoldStatus;
import com.ticketbooking.system.exception.ConflictException;
import com.ticketbooking.system.exception.HoldExpiredException;
import com.ticketbooking.system.exception.ValidationException;
import com.ticketbooking.system.exception.NotFoundException;
import com.ticketbooking.system.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class BookingService {

    private static final int HOLD_DURATION_MINUTES = 5;
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final EventRepository eventRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatHoldItemRepository seatHoldItemRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;

    public BookingService(EventRepository eventRepository,
                          SeatHoldRepository seatHoldRepository,
                          SeatHoldItemRepository seatHoldItemRepository,
                          BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository) {
        this.eventRepository = eventRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.seatHoldItemRepository = seatHoldItemRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
    }

    @Transactional
    public HoldResponse holdSeats(Long eventId, HoldSeatsRequest request) {
        int seatCount = request.seatNumbers() == null ? 0 : request.seatNumbers().size();
        log.info("Processing hold seats eventId={} userId={} seatCount={}", eventId, request.userId(), seatCount);
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));

        List<Integer> seats = normalizeAndValidateSeats(request.seatNumbers(), event.getTotalSeats());
        Set<Integer> occupied = getOccupiedSeats(eventId);

        for (Integer seat : seats) {
            if (occupied.contains(seat)) {
                throw new ConflictException("Seat " + seat + " is not available");
            }
        }

        SeatHold hold = new SeatHold();
        hold.setId(UUID.randomUUID().toString());
        hold.setEvent(event);
        hold.setUserId(request.userId());
        hold.setStatus(HoldStatus.ACTIVE);
        hold.setCreatedAt(LocalDateTime.now());
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(HOLD_DURATION_MINUTES));

        List<SeatHoldItem> items = new ArrayList<>();
        for (Integer seatNumber : seats) {
            SeatHoldItem item = new SeatHoldItem();
            item.setHold(hold);
            item.setSeatNumber(seatNumber);
            items.add(item);
        }
        hold.setSeats(items);

        SeatHold saved = seatHoldRepository.save(hold);
        log.info("Hold created holdId={} eventId={} userId={}", saved.getId(), eventId, saved.getUserId());
        return toHoldResponse(saved);
    }

    @Transactional
    public BookingResponse confirmBooking(ConfirmBookingRequest request) {
        log.info("Processing confirm booking holdId={}", request.holdId());
        SeatHold hold = seatHoldRepository.findById(request.holdId())
                .orElseThrow(() -> new NotFoundException("Hold not found: " + request.holdId()));

        Event event = eventRepository.findByIdForUpdate(hold.getEvent().getId())
                .orElseThrow(() -> new NotFoundException("Event not found: " + hold.getEvent().getId()));

        if (hold.getStatus() != HoldStatus.ACTIVE || hold.getExpiresAt().isBefore(LocalDateTime.now())) {
            hold.setStatus(HoldStatus.EXPIRED);
            throw new HoldExpiredException("Hold is expired or not active");
        }

        if (bookingRepository.existsByHoldIdAndUserIdAndStatus(request.holdId(), hold.getUserId(), BookingStatus.CONFIRMED)) {
            System.out.println(event.getId() + hold.getUserId());
            throw new ConflictException("User already has a confirmed booking for this event");
        }

        Set<Integer> currentlyBookedSeats = new HashSet<>(bookingSeatRepository
                .findSeatNumbersForBookingStatus(event.getId(), BookingStatus.CONFIRMED));
        List<Integer> holdSeatNumbers = hold.getSeats().stream().map(SeatHoldItem::getSeatNumber).toList();

        for (Integer seat : holdSeatNumbers) {
            if (currentlyBookedSeats.contains(seat)) {
                throw new ConflictException("Seat " + seat + " got booked while confirming. Please retry.");
            }
        }

        Booking booking = new Booking();
        booking.setEvent(event);
        booking.setUserId(hold.getUserId());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setHoldId(hold.getId());

        List<BookingSeat> bookingSeats = new ArrayList<>();
        for (Integer seatNumber : holdSeatNumbers) {
            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBooking(booking);
            bookingSeat.setSeatNumber(seatNumber);
            bookingSeats.add(bookingSeat);
        }
        booking.setSeats(bookingSeats);

        Booking savedBooking = bookingRepository.save(booking);
        hold.setStatus(HoldStatus.CONFIRMED);
        log.info("Booking confirmed bookingId={} holdId={} userId={}", savedBooking.getId(), hold.getId(), hold.getUserId());

        return toBookingResponse(savedBooking);
    }


    @Transactional(readOnly = true)
    public BookingsSummaryResponse listBookings() {
        log.info("Listing bookings with holds summary");
        List<BookingWithHoldsResponse> bookings = bookingRepository.findAll().stream()
                .map(this::toBookingWithHoldsResponse)
                .toList();
        List<HoldResponse> holds = listHolds(null, null);
        return new BookingsSummaryResponse(bookings, holds);
    }

    @Transactional(readOnly = true)
    public BookingWithHoldsResponse viewBooking(Long bookingId) {
        log.info("Viewing booking bookingId={}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        return toBookingWithHoldsResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        log.info("Canceling booking bookingId={}", bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new ConflictException("Booking already canceled");
        }

        booking.setStatus(BookingStatus.CANCELED);
        booking.setCanceledAt(LocalDateTime.now());
        log.info("Booking canceled bookingId={}", booking.getId());
        return toBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<HoldResponse> listHolds(Long eventId, String userId) {
        log.info("Listing holds eventId={} userId={}", eventId, userId);
        List<SeatHold> holds;
        if (eventId != null && userId != null) {
            holds = seatHoldRepository.findByStatusAndEventIdAndUserId(HoldStatus.ACTIVE, eventId, userId);
        } else if (eventId != null) {
            holds = seatHoldRepository.findByStatusAndEventId(HoldStatus.ACTIVE, eventId);
        } else if (userId != null) {
            holds = seatHoldRepository.findByStatusAndUserId(HoldStatus.ACTIVE, userId);
        } else {
            holds = seatHoldRepository.findByStatus(HoldStatus.ACTIVE);
        }

        return holds.stream()
                .map(this::toHoldResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> availabilityAll() {
        return eventRepository.findAll().stream()
                .map(event -> toAvailability(event.getId(), event))
                .toList();
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        return toAvailability(eventId, event);
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<SeatHold> expiredHolds = seatHoldRepository.findByStatusAndExpiresAtBefore(HoldStatus.ACTIVE, now);
        expiredHolds.forEach(hold -> hold.setStatus(HoldStatus.EXPIRED));
    }


    private AvailabilityResponse toAvailability(Long eventId, Event event) {
        long held = seatHoldItemRepository.countForActiveHolds(eventId, HoldStatus.ACTIVE, LocalDateTime.now());
        long booked = bookingSeatRepository.countForBookingStatus(eventId, BookingStatus.CONFIRMED);
        long available = Math.max(0, event.getTotalSeats() - held - booked);

        return new AvailabilityResponse(
                event.getId(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getTotalSeats(),
                held,
                booked,
                available
        );
    }

    private Set<Integer> getOccupiedSeats(Long eventId) {
        Set<Integer> occupied = new HashSet<>(bookingSeatRepository.findSeatNumbersForBookingStatus(eventId, BookingStatus.CONFIRMED));
        occupied.addAll(seatHoldItemRepository.findSeatNumbersForActiveHolds(eventId, HoldStatus.ACTIVE, LocalDateTime.now()));
        return occupied;
    }

    private List<Integer> normalizeAndValidateSeats(List<Integer> seats, int totalSeats) {
        if (seats == null || seats.isEmpty()) {
            throw new ValidationException("At least one seat must be requested");
        }

        Set<Integer> unique = new LinkedHashSet<>(seats);
        if (unique.size() != seats.size()) {
            throw new ValidationException("Duplicate seat numbers in request");
        }

        for (Integer seat : unique) {
            if (seat == null || seat < 1 || seat > totalSeats) {
                throw new ValidationException("Seat number out of range: " + seat);
            }
        }

        return new ArrayList<>(unique);
    }

    private HoldResponse toHoldResponse(SeatHold hold) {
        return new HoldResponse(
                hold.getId(),
                hold.getEvent().getId(),
                hold.getUserId(),
                hold.getExpiresAt(),
                hold.getSeats().stream().map(SeatHoldItem::getSeatNumber).sorted().toList()
        );
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getEvent().getId(),
                booking.getUserId(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getCanceledAt(),
                booking.getSeats().stream().map(BookingSeat::getSeatNumber).sorted().toList()
        );
    }

    private BookingWithHoldsResponse toBookingWithHoldsResponse(Booking booking) {
        List<String> holdIds = seatHoldRepository
                .findHoldIdsForEventAndUser(booking.getEvent().getId(), booking.getUserId(), HoldStatus.ACTIVE)
                .stream()
                .sorted()
                .toList();
        return new BookingWithHoldsResponse(
                booking.getId(),
                booking.getEvent().getId(),
                booking.getUserId(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getCanceledAt(),
                booking.getSeats().stream().map(BookingSeat::getSeatNumber).sorted().toList(),
                holdIds.size(),
                holdIds
        );
    }
}
