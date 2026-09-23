package com.moviebooking.event;

import com.moviebooking.domain.entity.Booking;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class BookingCancelledEvent extends ApplicationEvent {
    private final Booking booking;
    private final BigDecimal refundAmount;

    public BookingCancelledEvent(Object source, Booking booking, BigDecimal refundAmount) {
        super(source);
        this.booking = booking;
        this.refundAmount = refundAmount;
    }
}
