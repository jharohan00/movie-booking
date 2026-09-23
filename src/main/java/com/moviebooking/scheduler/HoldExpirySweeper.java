package com.moviebooking.scheduler;

import com.moviebooking.domain.entity.Booking;
import com.moviebooking.domain.entity.ShowSeat;
import com.moviebooking.domain.enums.BookingStatus;
import com.moviebooking.domain.enums.ShowSeatStatus;
import com.moviebooking.repository.BookingRepository;
import com.moviebooking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class HoldExpirySweeper {

    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;

    /**
     * Runs every 60 seconds. Releases expired HELD seats back to AVAILABLE
     * and cancels the associated PENDING bookings.
     */
    @Scheduled(fixedDelayString = "${app.booking.sweeper-interval-ms:60000}")
    @Transactional
    public void releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<ShowSeat> expired = showSeatRepository.findExpiredHolds(now);

        if (expired.isEmpty()) return;

        log.info("Hold sweeper: releasing {} expired holds", expired.size());

        expired.forEach(ss -> {
            ss.setStatus(ShowSeatStatus.AVAILABLE);
            ss.setHoldExpiresAt(null);
            ss.setHeldByUser(null);
        });
        showSeatRepository.saveAll(expired);

        // Cancel associated PENDING bookings
        // A booking is stale if ALL its seat holds have expired
        // We collect unique booking IDs from the expired seats via BookingItem
        // (simple approach: query PENDING bookings whose show seats are now AVAILABLE)
        List<Long> affectedShowIds = expired.stream()
                .map(ss -> ss.getShow().getId())
                .distinct()
                .toList();

        affectedShowIds.forEach(showId -> {
            List<Booking> pendingBookings = bookingRepository.findByShowIdAndStatus(showId, BookingStatus.PENDING);
            pendingBookings.forEach(b -> {
                // Check all items' seats are back to AVAILABLE (fully expired hold)
                boolean allExpired = b.getItems().stream()
                        .allMatch(item -> item.getShowSeat().getStatus() == ShowSeatStatus.AVAILABLE);
                if (allExpired) {
                    b.setStatus(BookingStatus.CANCELLED);
                    bookingRepository.save(b);
                    log.info("Cancelled stale PENDING booking {} (hold expired)", b.getId());
                }
            });
        });
    }
}
