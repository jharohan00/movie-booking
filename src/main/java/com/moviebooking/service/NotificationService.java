package com.moviebooking.service;

import com.moviebooking.domain.entity.*;
import com.moviebooking.domain.enums.NotificationType;
import com.moviebooking.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Async("notificationExecutor")
    @Transactional
    public void sendConfirmation(Booking booking) {
        String msg = String.format(
                "Your booking #%d for '%s' on %s is CONFIRMED. Total paid: ₹%.2f. Enjoy the show!",
                booking.getId(),
                booking.getShow().getMovie().getTitle(),
                booking.getShow().getStartTime(),
                booking.getTotalAmount()
        );
        save(booking.getUser(), booking, NotificationType.CONFIRMATION, msg);
        log.info("[NOTIFICATION] CONFIRMATION → {} | {}", booking.getUser().getEmail(), msg);
    }

    @Async("notificationExecutor")
    @Transactional
    public void sendCancellation(Booking booking, BigDecimal refundAmount) {
        String msg = String.format(
                "Your booking #%d for '%s' has been CANCELLED. Refund: ₹%.2f.",
                booking.getId(),
                booking.getShow().getMovie().getTitle(),
                refundAmount
        );
        save(booking.getUser(), booking, NotificationType.CANCELLATION, msg);
        log.info("[NOTIFICATION] CANCELLATION → {} | {}", booking.getUser().getEmail(), msg);
    }

    @Async("notificationExecutor")
    @Transactional
    public void sendReminder(Booking booking) {
        String msg = String.format(
                "Reminder: Your booking #%d for '%s' starts at %s. Don't be late!",
                booking.getId(),
                booking.getShow().getMovie().getTitle(),
                booking.getShow().getStartTime()
        );
        save(booking.getUser(), booking, NotificationType.REMINDER, msg);
        log.info("[NOTIFICATION] REMINDER → {} | {}", booking.getUser().getEmail(), msg);
    }

    private void save(User user, Booking booking, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .booking(booking)
                .notificationType(type)
                .message(message)
                .build();
        notificationRepository.save(notification);
    }
}
