package com.ticketbooking.system.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "seat_hold_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hold_seat", columnNames = {"hold_id", "seat_number"})
})
public class SeatHoldItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hold_id", nullable = false)
    private SeatHold hold;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SeatHold getHold() { return hold; }
    public void setHold(SeatHold hold) { this.hold = hold; }
    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
}
