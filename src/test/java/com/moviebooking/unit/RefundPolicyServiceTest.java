package com.moviebooking.unit;

import com.moviebooking.domain.entity.*;
import com.moviebooking.domain.enums.*;
import com.moviebooking.repository.*;
import com.moviebooking.service.RefundPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundPolicyServiceTest {

    @Mock RefundPolicyRepository refundPolicyRepository;
    @Mock TheaterRepository theaterRepository;
    @Mock ShowRepository showRepository;

    @InjectMocks RefundPolicyService refundPolicyService;

    private Booking booking;
    private Theater theater;
    private Show show;

    @BeforeEach
    void setUp() {
        City city = City.builder().id(1L).name("Delhi").build();
        theater = Theater.builder().id(1L).city(city).name("INOX").build();
        Screen screen = Screen.builder().id(1L).theater(theater).build();
        Movie movie = Movie.builder().id(1L).title("Avatar").build();

        show = Show.builder()
                .id(1L)
                .screen(screen)
                .movie(movie)
                .startTime(LocalDateTime.now().plusHours(50))  // 50 hours from now
                .build();

        User user = User.builder().id(1L).email("u@test.com").build();

        booking = Booking.builder()
                .id(1L)
                .user(user)
                .show(show)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("300.00"))
                .discountAmount(BigDecimal.ZERO)
                .items(List.of())
                .build();
    }

    @Test
    void computeRefundAmount_fullRefundWhenCancelledWellInAdvance() {
        // Show is 50h away; policy: >48h → 100%, >24h → 50%, else 0%
        List<RefundPolicy> policies = List.of(
                RefundPolicy.builder().hoursBeforeShow(48).refundPercent(new BigDecimal("100")).build(),
                RefundPolicy.builder().hoursBeforeShow(24).refundPercent(new BigDecimal("50")).build(),
                RefundPolicy.builder().hoursBeforeShow(0).refundPercent(BigDecimal.ZERO).build()
        );

        when(refundPolicyRepository.findByShowId(1L)).thenReturn(policies);

        BigDecimal refund = refundPolicyService.computeRefundAmount(booking);

        assertThat(refund).isEqualByComparingTo("300.00");
    }

    @Test
    void computeRefundAmount_partialRefundInMiddleBracket() {
        // Show is 30h away; policy brackets same as above → 50% bracket applies
        show.setStartTime(LocalDateTime.now().plusHours(30));

        List<RefundPolicy> policies = List.of(
                RefundPolicy.builder().hoursBeforeShow(48).refundPercent(new BigDecimal("100")).build(),
                RefundPolicy.builder().hoursBeforeShow(24).refundPercent(new BigDecimal("50")).build(),
                RefundPolicy.builder().hoursBeforeShow(0).refundPercent(BigDecimal.ZERO).build()
        );

        when(refundPolicyRepository.findByShowId(1L)).thenReturn(policies);

        BigDecimal refund = refundPolicyService.computeRefundAmount(booking);

        assertThat(refund).isEqualByComparingTo("150.00");
    }

    @Test
    void computeRefundAmount_noRefundWithinDay() {
        // Show is 6h away → 0% bracket
        show.setStartTime(LocalDateTime.now().plusHours(6));

        List<RefundPolicy> policies = List.of(
                RefundPolicy.builder().hoursBeforeShow(48).refundPercent(new BigDecimal("100")).build(),
                RefundPolicy.builder().hoursBeforeShow(24).refundPercent(new BigDecimal("50")).build(),
                RefundPolicy.builder().hoursBeforeShow(0).refundPercent(BigDecimal.ZERO).build()
        );

        when(refundPolicyRepository.findByShowId(1L)).thenReturn(policies);

        BigDecimal refund = refundPolicyService.computeRefundAmount(booking);

        assertThat(refund).isEqualByComparingTo("0.00");
    }

    @Test
    void computeRefundAmount_fallsBackToGlobalPolicy() {
        // No show or theater policy
        when(refundPolicyRepository.findByShowId(1L)).thenReturn(List.of());
        when(refundPolicyRepository.findByTheaterId(1L)).thenReturn(List.of());
        when(refundPolicyRepository.findGlobalPolicies()).thenReturn(
                List.of(RefundPolicy.builder().hoursBeforeShow(0).refundPercent(new BigDecimal("100")).build())
        );

        BigDecimal refund = refundPolicyService.computeRefundAmount(booking);

        assertThat(refund).isEqualByComparingTo("300.00");
    }
}
