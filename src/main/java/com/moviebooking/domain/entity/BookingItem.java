package com.moviebooking.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id", "show_seat_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_seat_id", nullable = false)
    private ShowSeat showSeat;

    @Column(name = "price_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePaid;
}
