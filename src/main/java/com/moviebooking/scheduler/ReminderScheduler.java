package com.moviebooking.scheduler;

import com.moviebooking.domain.entity.Booking;
import com.moviebooking.domain.enums.BookingStatus;
import com.moviebooking.repository.BookingRepository;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @Value("${app.reminder.hours-before-show:24}")
    private int hoursBeforeShow;

    /**
     * Runs once per hour. Sends reminder notifications for confirmed bookings
     * whose show starts in the next [hoursBeforeShow] hours (within a 1-hour window).
     */
    @Scheduled(cron = "0 0 * * * *")   // every hour on the hour
    public void sendReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(hoursBeforeShow - 1);
        LocalDateTime to   = LocalDateTime.now().plusHours(hoursBeforeShow);

        List<Booking> bookingsToRemind = showRepository.findUpcomingShows(from, to)
                .stream()
                .flatMap(show -> bookingRepository.findByShowIdAndStatus(show.getId(), BookingStatus.CONFIRMED).stream())
                .toList();

        log.info("Reminder scheduler: {} bookings to remind", bookingsToRemind.size());
        bookingsToRemind.forEach(notificationService::sendReminder);
    }
}
