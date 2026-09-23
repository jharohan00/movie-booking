package com.moviebooking.service;

import com.moviebooking.domain.entity.*;
import com.moviebooking.domain.enums.PaymentStatus;
import com.moviebooking.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${app.payment.mock-failure-rate:0.0}")
    private double mockFailureRate;

    @Transactional
    public Payment processPayment(Booking booking) {
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .build();

        boolean success = Math.random() >= mockFailureRate;
        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionRef("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            log.info("Payment SUCCESS for booking {} | ref={} | amount={}",
                    booking.getId(), payment.getTransactionRef(), payment.getAmount());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            log.warn("Payment FAILED for booking {}", booking.getId());
        }

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment processRefund(Booking booking, BigDecimal refundAmount) {
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElse(null);

        if (payment == null || payment.getStatus() != PaymentStatus.SUCCESS) {
            log.warn("No successful payment found for booking {} — skipping refund", booking.getId());
            return null;
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundAmount(refundAmount);
        log.info("Refund processed for booking {} | refund amount={}", booking.getId(), refundAmount);
        return paymentRepository.save(payment);
    }
}
