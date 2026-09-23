package com.moviebooking.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "refund_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** If non-null, policy applies to this specific theater. Null = global default. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theater_id")
    private Theater theater;

    /** If non-null, this is a show-level override (takes highest priority). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id")
    private Show show;

    /**
     * Cancellations made this many hours (or more) before show start qualify for
     * refundPercent. Multiple rows with different thresholds form bracket tiers.
     */
    @Column(name = "hours_before_show", nullable = false)
    private int hoursBeforeShow;

    @Column(name = "refund_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercent;
}
