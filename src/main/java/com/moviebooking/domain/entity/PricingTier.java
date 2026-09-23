package com.moviebooking.domain.entity;

import com.moviebooking.domain.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_tiers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seat_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PricingTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "weekend_multiplier", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal weekendMultiplier = BigDecimal.ONE;

    @Column(name = "weekend_pricing_enabled", nullable = false)
    @Builder.Default
    private boolean weekendPricingEnabled = false;
}
