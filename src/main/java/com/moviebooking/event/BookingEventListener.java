package com.moviebooking.event;

import com.moviebooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void onConfirmed(BookingConfirmedEvent event) {
        notificationService.sendConfirmation(event.getBooking());
    }

    @EventListener
    public void onCancelled(BookingCancelledEvent event) {
        notificationService.sendCancellation(event.getBooking(), event.getRefundAmount());
    }
}
