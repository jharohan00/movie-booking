package com.moviebooking.unit;

import com.moviebooking.domain.entity.*;
import com.moviebooking.domain.enums.*;
import com.moviebooking.exception.InvalidDiscountCodeException;
import com.moviebooking.repository.*;
import com.moviebooking.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock PricingTierRepository pricingTierRepository;
    @Mock DiscountCodeRepository discountCodeRepository;
    @Mock ShowSeatRepository showSeatRepository;
    @Mock ShowRepository showRepository;

    @InjectMocks PricingService pricingService;

    private Show weekdayShow;
    private Show weekendShow;

    @BeforeEach
    void setUp() {
        // Find the next Monday (weekday)
        LocalDateTime nextMonday = LocalDateTime.now()
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .withHour(14).withMinute(0);
        // Find the next Saturday (weekend)
        LocalDateTime nextSaturday = LocalDateTime.now()
                .with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
                .withHour(14).withMinute(0);

        Screen screen = Screen.builder().id(1L).theater(Theater.builder().id(1L).city(City.builder().id(1L).build()).build()).build();
        weekdayShow = Show.builder().id(1L).screen(screen).startTime(nextMonday).build();
        weekendShow = Show.builder().id(2L).screen(screen).startTime(nextSaturday).build();
    }

    @Test
    void calculatePrice_weekdayNoMultiplier() {
        PricingTier tier = PricingTier.builder()
                .basePrice(new BigDecimal("200.00"))
                .weekendPricingEnabled(false)
                .weekendMultiplier(new BigDecimal("1.5"))
                .seatType(SeatType.REGULAR)
                .build();
        when(pricingTierRepository.findByShowIdAndSeatType(1L, SeatType.REGULAR)).thenReturn(Optional.of(tier));

        BigDecimal price = pricingService.calculatePrice(weekdayShow, SeatType.REGULAR);

        assertThat(price).isEqualByComparingTo("200.00");
    }

    @Test
    void calculatePrice_weekendAppliesMultiplier() {
        PricingTier tier = PricingTier.builder()
                .basePrice(new BigDecimal("200.00"))
                .weekendPricingEnabled(true)
                .weekendMultiplier(new BigDecimal("1.5"))
                .seatType(SeatType.REGULAR)
                .build();
        when(pricingTierRepository.findByShowIdAndSeatType(2L, SeatType.REGULAR)).thenReturn(Optional.of(tier));

        BigDecimal price = pricingService.calculatePrice(weekendShow, SeatType.REGULAR);

        assertThat(price).isEqualByComparingTo("300.00");
    }

    @Test
    void calculatePrice_returnsDefaultWhenNoPricingConfigured() {
        when(pricingTierRepository.findByShowIdAndSeatType(1L, SeatType.PREMIUM)).thenReturn(Optional.empty());

        BigDecimal price = pricingService.calculatePrice(weekdayShow, SeatType.PREMIUM);

        assertThat(price).isEqualByComparingTo("300.00"); // default premium price
    }

    @Test
    void computeDiscount_flat() {
        DiscountCode dc = DiscountCode.builder()
                .discountType(DiscountType.FLAT)
                .value(new BigDecimal("50.00"))
                .build();

        BigDecimal discount = pricingService.computeDiscount(dc, new BigDecimal("300.00"));

        assertThat(discount).isEqualByComparingTo("50.00");
    }

    @Test
    void computeDiscount_flatCappedAtSubtotal() {
        DiscountCode dc = DiscountCode.builder()
                .discountType(DiscountType.FLAT)
                .value(new BigDecimal("500.00"))
                .build();

        BigDecimal discount = pricingService.computeDiscount(dc, new BigDecimal("100.00"));

        assertThat(discount).isEqualByComparingTo("100.00");
    }

    @Test
    void computeDiscount_percent() {
        DiscountCode dc = DiscountCode.builder()
                .discountType(DiscountType.PERCENT)
                .value(new BigDecimal("20"))
                .build();

        BigDecimal discount = pricingService.computeDiscount(dc, new BigDecimal("300.00"));

        assertThat(discount).isEqualByComparingTo("60.00");
    }

    @Test
    void validateDiscountCode_throwsWhenExpired() {
        DiscountCode dc = DiscountCode.builder()
                .code("EXPIRED")
                .discountType(DiscountType.FLAT)
                .value(BigDecimal.TEN)
                .validFrom(LocalDateTime.now().minusDays(10))
                .validTo(LocalDateTime.now().minusDays(1))
                .build();

        when(discountCodeRepository.findByCodeWithLock("EXPIRED")).thenReturn(Optional.of(dc));

        assertThatThrownBy(() ->
                pricingService.validateAndLockDiscountCode("EXPIRED", 1L, LocalDateTime.now()))
                .isInstanceOf(InvalidDiscountCodeException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void validateDiscountCode_throwsWhenUsageLimitReached() {
        DiscountCode dc = DiscountCode.builder()
                .code("USED")
                .discountType(DiscountType.FLAT)
                .value(BigDecimal.TEN)
                .maxUses(5)
                .usedCount(5)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .build();

        when(discountCodeRepository.findByCodeWithLock("USED")).thenReturn(Optional.of(dc));

        assertThatThrownBy(() ->
                pricingService.validateAndLockDiscountCode("USED", 1L, LocalDateTime.now()))
                .isInstanceOf(InvalidDiscountCodeException.class)
                .hasMessageContaining("usage limit");
    }
}
