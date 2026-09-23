package com.moviebooking.domain.entity;

import com.moviebooking.domain.enums.ShowSeatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "show_seats",
       uniqueConstraints = @UniqueConstraint(columnNames = {"show_id", "seat_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShowSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShowSeatStatus status = ShowSeatStatus.AVAILABLE;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "held_by_user_id")
    private User heldByUser;

    @Version
    private Long version;
}
