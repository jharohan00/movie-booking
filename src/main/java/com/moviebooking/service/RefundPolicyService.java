package com.moviebooking.service;

import com.moviebooking.domain.entity.*;
import com.moviebooking.dto.request.CreateRefundPolicyRequest;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundPolicyService {

    private final RefundPolicyRepository refundPolicyRepository;
    private final TheaterRepository theaterRepository;
    private final ShowRepository showRepository;

    @Transactional
    public List<RefundPolicy> createPolicy(CreateRefundPolicyRequest request) {
        Theater theater = null;
        Show show = null;

        if (request.getShowId() != null) {
            show = showRepository.findById(request.getShowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + request.getShowId()));
        } else if (request.getTheaterId() != null) {
            theater = theaterRepository.findById(request.getTheaterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Theater not found: " + request.getTheaterId()));
        }

        final Theater finalTheater = theater;
        final Show finalShow = show;

        List<RefundPolicy> policies = request.getBrackets().stream()
                .map(b -> RefundPolicy.builder()
                        .theater(finalTheater)
                        .show(finalShow)
                        .hoursBeforeShow(b.getHoursBeforeShow())
                        .refundPercent(b.getRefundPercent())
                        .build())
                .toList();

        return refundPolicyRepository.saveAll(policies);
    }

    public List<RefundPolicy> getAllPolicies() {
        return refundPolicyRepository.findAll();
    }

    /**
     * Calculates the refund amount for a booking based on the applicable policy.
     * Priority: show-level > theater-level > global.
     */
    public BigDecimal computeRefundAmount(Booking booking) {
        LocalDateTime showStart = booking.getShow().getStartTime();
        long hoursUntilShow = ChronoUnit.HOURS.between(LocalDateTime.now(), showStart);

        // Show-level override
        List<RefundPolicy> policies = refundPolicyRepository.findByShowId(booking.getShow().getId());

        // Fall back to theater-level
        if (policies.isEmpty()) {
            Long theaterId = booking.getShow().getScreen().getTheater().getId();
            policies = refundPolicyRepository.findByTheaterId(theaterId);
        }

        // Fall back to global
        if (policies.isEmpty()) {
            policies = refundPolicyRepository.findGlobalPolicies();
        }

        // Policies are sorted DESC by hoursBeforeShow — find the first bracket that qualifies
        BigDecimal refundPercent = policies.stream()
                .filter(p -> hoursUntilShow >= p.getHoursBeforeShow())
                .map(RefundPolicy::getRefundPercent)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        BigDecimal totalPaid = booking.getTotalAmount();
        BigDecimal refund = totalPaid.multiply(refundPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        log.info("Refund calc: booking={} hoursUntilShow={} refundPercent={} refund={}",
                booking.getId(), hoursUntilShow, refundPercent, refund);
        return refund;
    }
}
