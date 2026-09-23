package com.moviebooking.service;

import com.moviebooking.domain.entity.*;
import com.moviebooking.domain.enums.DiscountType;
import com.moviebooking.domain.enums.SeatType;
import com.moviebooking.dto.request.*;
import com.moviebooking.dto.response.*;
import com.moviebooking.exception.InvalidDiscountCodeException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingTierRepository pricingTierRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowRepository showRepository;

    @Transactional
    public List<PricingTier> setPricing(Long showId, SetPricingRequest request) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        // Remove existing tiers for this show
        pricingTierRepository.deleteAll(pricingTierRepository.findByShowId(showId));

        List<PricingTier> tiers = request.getTiers().stream()
                .map(t -> PricingTier.builder()
                        .show(show)
                        .seatType(t.getSeatType())
                        .basePrice(t.getBasePrice())
                        .weekendPricingEnabled(t.isWeekendPricingEnabled())
                        .weekendMultiplier(t.getWeekendMultiplier() != null ? t.getWeekendMultiplier() : BigDecimal.ONE)
                        .build())
                .toList();

        return pricingTierRepository.saveAll(tiers);
    }

    /**
     * Calculate the effective price for a seat in a given show.
     * Weekend multiplier applies on Friday, Saturday, Sunday.
     */
    public BigDecimal calculatePrice(Show show, SeatType seatType) {
        Optional<PricingTier> tierOpt = pricingTierRepository.findByShowIdAndSeatType(show.getId(), seatType);
        if (tierOpt.isEmpty()) {
            // Default prices if not configured
            return seatType == SeatType.PREMIUM ? new BigDecimal("300.00") : new BigDecimal("150.00");
        }
        PricingTier tier = tierOpt.get();
        BigDecimal price = tier.getBasePrice();

        if (tier.isWeekendPricingEnabled() && isWeekend(show.getStartTime())) {
            price = price.multiply(tier.getWeekendMultiplier()).setScale(2, RoundingMode.HALF_UP);
        }
        return price;
    }

    /**
     * Validates and returns the discount code (locked for update — caller must be inside a tx).
     * Returns empty if discountCode string is null/blank.
     */
    public Optional<DiscountCode> validateAndLockDiscountCode(String code, Long showId, LocalDateTime now) {
        if (code == null || code.isBlank()) return Optional.empty();

        DiscountCode dc = discountCodeRepository.findByCodeWithLock(code)
                .orElseThrow(() -> new InvalidDiscountCodeException("Discount code not found: " + code));

        if (now.isBefore(dc.getValidFrom()) || now.isAfter(dc.getValidTo())) {
            throw new InvalidDiscountCodeException("Discount code is expired or not yet valid: " + code);
        }
        if (dc.getMaxUses() != null && dc.getUsedCount() >= dc.getMaxUses()) {
            throw new InvalidDiscountCodeException("Discount code usage limit reached: " + code);
        }
        if (dc.getShow() != null && !dc.getShow().getId().equals(showId)) {
            throw new InvalidDiscountCodeException("Discount code is not valid for this show: " + code);
        }

        return Optional.of(dc);
    }

    /**
     * Compute discount amount for a given subtotal.
     */
    public BigDecimal computeDiscount(DiscountCode dc, BigDecimal subtotal) {
        if (dc == null) return BigDecimal.ZERO;
        if (dc.getDiscountType() == DiscountType.FLAT) {
            return dc.getValue().min(subtotal);
        } else {
            return subtotal.multiply(dc.getValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
    }

    public void incrementDiscountUsage(DiscountCode dc) {
        dc.setUsedCount(dc.getUsedCount() + 1);
        discountCodeRepository.save(dc);
    }

    @Transactional
    public DiscountCode createDiscountCode(CreateDiscountCodeRequest request) {
        Show show = null;
        if (request.getShowId() != null) {
            show = showRepository.findById(request.getShowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + request.getShowId()));
        }

        DiscountCode dc = DiscountCode.builder()
                .code(request.getCode().toUpperCase())
                .discountType(request.getDiscountType())
                .value(request.getValue())
                .maxUses(request.getMaxUses())
                .show(show)
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .build();

        return discountCodeRepository.save(dc);
    }

    public List<DiscountCode> getAllDiscountCodes() {
        return discountCodeRepository.findAll();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isWeekend(LocalDateTime dt) {
        DayOfWeek day = dt.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public ShowResponse toShowResponse(Show show) {
        List<PricingTier> tiers = pricingTierRepository.findByShowId(show.getId());
        long available = showSeatRepository.countByShowIdAndStatus(show.getId(),
                com.moviebooking.domain.enums.ShowSeatStatus.AVAILABLE);

        Screen screen = show.getScreen();
        Theater theater = screen.getTheater();
        City city = theater.getCity();

        List<ShowResponse.PricingTierInfo> tierInfos = tiers.stream().map(t -> {
            BigDecimal weekendPrice = t.isWeekendPricingEnabled()
                    ? t.getBasePrice().multiply(t.getWeekendMultiplier()).setScale(2, RoundingMode.HALF_UP)
                    : t.getBasePrice();
            return ShowResponse.PricingTierInfo.builder()
                    .seatType(t.getSeatType().name())
                    .basePrice(t.getBasePrice())
                    .weekendPrice(weekendPrice)
                    .weekendPricingEnabled(t.isWeekendPricingEnabled())
                    .build();
        }).toList();

        return ShowResponse.builder()
                .id(show.getId())
                .movieTitle(show.getMovie().getTitle())
                .durationMins(show.getMovie().getDurationMins())
                .genre(show.getMovie().getGenre())
                .language(show.getMovie().getLanguage())
                .theaterName(theater.getName())
                .screenName(screen.getName())
                .cityName(city.getName())
                .startTime(show.getStartTime())
                .endTime(show.getEndTime())
                .status(show.getStatus().name())
                .availableSeats(available)
                .pricingTiers(tierInfos)
                .build();
    }
}
