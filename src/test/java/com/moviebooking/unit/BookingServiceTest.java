package com.moviebooking.unit;

import com.moviebooking.domain.entity.*;
import com.moviebooking.domain.enums.*;
import com.moviebooking.dto.request.HoldSeatsRequest;
import com.moviebooking.event.BookingCancelledEvent;
import com.moviebooking.event.BookingConfirmedEvent;
import com.moviebooking.exception.BookingException;
import com.moviebooking.exception.SeatNotAvailableException;
import com.moviebooking.repository.*;
import com.moviebooking.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock ShowRepository showRepository;
    @Mock ShowSeatRepository showSeatRepository;
    @Mock UserRepository userRepository;
    @Mock PaymentService paymentService;
    @Mock PricingService pricingService;
    @Mock RefundPolicyService refundPolicyService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks BookingService bookingService;

    private User user;
    private Show show;
    private Screen screen;
    private Theater theater;
    private City city;
    private Seat seat;
    private ShowSeat showSeat;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "holdTtlMinutes", 10);

        city = City.builder().id(1L).name("Mumbai").build();
        theater = Theater.builder().id(1L).city(city).name("PVR").build();
        screen = Screen.builder().id(1L).theater(theater).name("Screen 1").build();
        Movie movie = Movie.builder().id(1L).title("Inception").build();
        show = Show.builder()
                .id(1L)
                .screen(screen)
                .movie(movie)
                .startTime(LocalDateTime.now().plusDays(3))
                .endTime(LocalDateTime.now().plusDays(3).plusHours(2))
                .status(ShowStatus.SCHEDULED)
                .build();
        user = User.builder().id(1L).email("user@test.com").role(Role.CUSTOMER).build();
        seat = Seat.builder().id(1L).screen(screen).rowLabel("A").seatNumber(1).seatType(SeatType.REGULAR).build();
        showSeat = ShowSeat.builder().id(10L).show(show).seat(seat).status(ShowSeatStatus.AVAILABLE).version(0L).build();

        // Mock security context
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    }

    @Test
    void holdSeats_successfulHold() {
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(showSeatRepository.findByIdsWithLock(List.of(10L))).thenReturn(List.of(showSeat));
        when(pricingService.validateAndLockDiscountCode(any(), any(), any())).thenReturn(Optional.empty());
        when(pricingService.calculatePrice(any(), any())).thenReturn(new BigDecimal("150.00"));
        when(pricingService.computeDiscount(null, new BigDecimal("150.00"))).thenReturn(BigDecimal.ZERO);
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            ReflectionTestUtils.setField(b, "id", 100L);
            return b;
        });
        when(showSeatRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        HoldSeatsRequest request = new HoldSeatsRequest();
        request.setShowId(1L);
        request.setShowSeatIds(List.of(10L));

        var response = bookingService.holdSeats(request);

        assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);
        assertThat(showSeat.getHoldExpiresAt()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("150.00");
    }

    @Test
    void holdSeats_failsWhenSeatAlreadyHeld() {
        showSeat.setStatus(ShowSeatStatus.HELD);
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(showSeatRepository.findByIdsWithLock(List.of(10L))).thenReturn(List.of(showSeat));
        when(pricingService.validateAndLockDiscountCode(any(), any(), any())).thenReturn(Optional.empty());

        HoldSeatsRequest request = new HoldSeatsRequest();
        request.setShowId(1L);
        request.setShowSeatIds(List.of(10L));

        assertThatThrownBy(() -> bookingService.holdSeats(request))
                .isInstanceOf(SeatNotAvailableException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void holdSeats_failsForCancelledShow() {
        show.setStatus(ShowStatus.CANCELLED);
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));

        HoldSeatsRequest request = new HoldSeatsRequest();
        request.setShowId(1L);
        request.setShowSeatIds(List.of(10L));

        assertThatThrownBy(() -> bookingService.holdSeats(request))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void holdSeats_failsForDuplicateSeatIds() {
        when(showRepository.findById(1L)).thenReturn(Optional.of(show));

        HoldSeatsRequest request = new HoldSeatsRequest();
        request.setShowId(1L);
        request.setShowSeatIds(List.of(10L, 10L));

        assertThatThrownBy(() -> bookingService.holdSeats(request))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void confirmBooking_failsWhenHoldExpired() {
        showSeat.setStatus(ShowSeatStatus.HELD);
        showSeat.setHoldExpiresAt(LocalDateTime.now().minusMinutes(5)); // expired

        BookingItem item = BookingItem.builder()
                .showSeat(showSeat)
                .pricePaid(new BigDecimal("150.00"))
                .build();

        Booking booking = Booking.builder()
                .id(100L)
                .user(user)
                .show(show)
                .status(BookingStatus.PENDING)
                .totalAmount(new BigDecimal("150.00"))
                .discountAmount(BigDecimal.ZERO)
                .items(List.of(item))
                .build();
        item.setBooking(booking);

        when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBooking(100L))
                .isInstanceOf(BookingException.class)
                .hasMessageContaining("Hold has expired");
    }

    @Test
    void cancelBooking_releasesSeatsAndPublishesEvent() {
        showSeat.setStatus(ShowSeatStatus.BOOKED);

        BookingItem item = BookingItem.builder()
                .showSeat(showSeat)
                .pricePaid(new BigDecimal("150.00"))
                .build();

        Booking booking = Booking.builder()
                .id(100L)
                .user(user)
                .show(show)
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("150.00"))
                .discountAmount(BigDecimal.ZERO)
                .items(new java.util.ArrayList<>(List.of(item)))
                .build();
        item.setBooking(booking);

        when(bookingRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(booking));
        when(refundPolicyService.computeRefundAmount(booking)).thenReturn(new BigDecimal("150.00"));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(showSeatRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = bookingService.cancelBooking(100L);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);
        verify(eventPublisher).publishEvent(any(BookingCancelledEvent.class));
    }
}
