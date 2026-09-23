package com.moviebooking.event;

import com.moviebooking.domain.entity.Booking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BookingConfirmedEvent extends ApplicationEvent {
    private final Booking booking;

    public BookingConfirmedEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
    }
}
