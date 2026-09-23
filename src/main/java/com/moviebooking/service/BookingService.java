package com.moviebooking.service;

import com.moviebooking.domain.entity.*;
import com.moviebooking.domain.enums.*;
import com.moviebooking.dto.request.HoldSeatsRequest;
import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.dto.response.SeatMapResponse;
import com.moviebooking.event.BookingCancelledEvent;
import com.moviebooking.event.BookingConfirmedEvent;
import com.moviebooking.exception.*;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final PricingService pricingService;
    private final RefundPolicyService refundPolicyService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.booking.hold-ttl-minutes}")
    private int holdTtlMinutes;

    // ─────────────────────────────────────────────────────────────────────────
    // HOLD — Phase 1
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Acquires a pessimistic write lock on the requested ShowSeat rows and
     * transitions them from AVAILABLE → HELD.  Returns a PENDING Booking.
     */
    @Transactional
    public BookingResponse holdSeats(HoldSeatsRequest request) {
        User user = currentUser();
        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + request.getShowId()));

        if (show.getStatus() == ShowStatus.CANCELLED) {
            throw new BookingException("Cannot book a cancelled show");
        }
        if (show.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BookingException("Show has already started");
        }
        if (request.getShowSeatIds().size() != new HashSet<>(request.getShowSeatIds()).size()) {
            throw new BookingException("Duplicate seat IDs in request");
        }

        // Acquire row-level exclusive lock (ordered by id to prevent deadlocks)
        List<ShowSeat> lockedSeats = showSeatRepository.findByIdsWithLock(request.getShowSeatIds());

        if (lockedSeats.size() != request.getShowSeatIds().size()) {
            throw new ResourceNotFoundException("One or more ShowSeat IDs not found");
        }

        // Validate all belong to this show
        lockedSeats.forEach(ss -> {
            if (!ss.getShow().getId().equals(request.getShowId())) {
                throw new BookingException("ShowSeat " + ss.getId() + " does not belong to show " + request.getShowId());
            }
        });

        // Check availability
        List<ShowSeat> unavailable = lockedSeats.stream()
                .filter(ss -> ss.getStatus() != ShowSeatStatus.AVAILABLE)
                .toList();
        if (!unavailable.isEmpty()) {
            String ids = unavailable.stream().map(ss -> String.valueOf(ss.getId())).collect(Collectors.joining(", "));
            throw new SeatNotAvailableException("Seats not available: " + ids);
        }

        // Validate discount code upfront (inside same tx so lock is held)
        DiscountCode discountCode = null;
        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            discountCode = pricingService.validateAndLockDiscountCode(
                    request.getDiscountCode(), show.getId(), LocalDateTime.now()
            ).orElse(null);
        }

        // Compute prices per seat
        LocalDateTime holdExpiry = LocalDateTime.now().plusMinutes(holdTtlMinutes);
        Map<ShowSeat, BigDecimal> seatPrices = new LinkedHashMap<>();
        for (ShowSeat ss : lockedSeats) {
            BigDecimal price = pricingService.calculatePrice(show, ss.getSeat().getSeatType());
            seatPrices.put(ss, price);
            ss.setStatus(ShowSeatStatus.HELD);
            ss.setHoldExpiresAt(holdExpiry);
            ss.setHeldByUser(user);
        }
        showSeatRepository.saveAll(lockedSeats);

        // Compute totals
        BigDecimal subtotal = seatPrices.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = pricingService.computeDiscount(discountCode, subtotal);
        BigDecimal total = subtotal.subtract(discountAmount);

        // Create booking
        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .discountCode(discountCode)
                .status(BookingStatus.PENDING)
                .totalAmount(total)
                .discountAmount(discountAmount)
                .build();
        booking = bookingRepository.save(booking);

        // Create booking items
        List<BookingItem> items = new ArrayList<>();
        for (Map.Entry<ShowSeat, BigDecimal> entry : seatPrices.entrySet()) {
            items.add(BookingItem.builder()
                    .booking(booking)
                    .showSeat(entry.getKey())
                    .pricePaid(entry.getValue())
                    .build());
        }
        booking.getItems().addAll(items);
        booking = bookingRepository.save(booking);

        return toResponse(booking, holdExpiry);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRM — Phase 2
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        User user = currentUser();
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new BookingException("You can only confirm your own bookings");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingException("Booking is not in PENDING state: " + booking.getStatus());
        }

        // Check hold expiry
        booking.getItems().forEach(item -> {
            ShowSeat ss = item.getShowSeat();
            if (ss.getStatus() != ShowSeatStatus.HELD
                    || ss.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BookingException("Hold has expired. Please restart the booking.");
            }
        });

        // Process payment
        Payment payment = paymentService.processPayment(booking);
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentFailedException("Payment failed. Please try again.");
        }

        // Promote seats to BOOKED
        List<ShowSeat> seats = booking.getItems().stream()
                .map(BookingItem::getShowSeat)
                .toList();
        seats.forEach(ss -> {
            ss.setStatus(ShowSeatStatus.BOOKED);
            ss.setHoldExpiresAt(null);
            ss.setHeldByUser(null);
        });
        showSeatRepository.saveAll(seats);

        // Increment discount usage
        if (booking.getDiscountCode() != null) {
            pricingService.incrementDiscountUsage(booking.getDiscountCode());
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        // Publish async event — does NOT block the HTTP response
        eventPublisher.publishEvent(new BookingConfirmedEvent(this, booking));

        return toResponse(booking, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        User user = currentUser();
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new BookingException("You can only cancel your own bookings");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }

        // Release seats
        List<ShowSeat> seats = booking.getItems().stream()
                .map(BookingItem::getShowSeat)
                .toList();
        seats.forEach(ss -> {
            ss.setStatus(ShowSeatStatus.AVAILABLE);
            ss.setHoldExpiresAt(null);
            ss.setHeldByUser(null);
        });
        showSeatRepository.saveAll(seats);

        // Compute & process refund (only for CONFIRMED bookings that had a payment)
        BigDecimal refundAmount = BigDecimal.ZERO;
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            refundAmount = refundPolicyService.computeRefundAmount(booking);
            paymentService.processRefund(booking, refundAmount);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingCancelledEvent(this, booking, refundAmount));

        return toResponse(booking, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERIES
    // ─────────────────────────────────────────────────────────────────────────

    public BookingResponse getBooking(Long bookingId) {
        User user = currentUser();
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new BookingException("Access denied to booking: " + bookingId);
        }
        return toResponse(booking, null);
    }

    public List<BookingResponse> getMyBookings() {
        User user = currentUser();
        return bookingRepository.findByUserId(user.getId())
                .stream()
                .map(b -> toResponse(b, null))
                .toList();
    }

    public SeatMapResponse getSeatMap(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));

        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);

        List<SeatMapResponse.SeatInfo> seatInfos = showSeats.stream().map(ss -> {
            BigDecimal price = pricingService.calculatePrice(show, ss.getSeat().getSeatType());
            return SeatMapResponse.SeatInfo.builder()
                    .showSeatId(ss.getId())
                    .seatId(ss.getSeat().getId())
                    .rowLabel(ss.getSeat().getRowLabel())
                    .seatNumber(ss.getSeat().getSeatNumber())
                    .seatType(ss.getSeat().getSeatType())
                    .status(ss.getStatus())
                    .price(price)
                    .build();
        }).toList();

        long available = seatInfos.stream()
                .filter(s -> s.getStatus() == ShowSeatStatus.AVAILABLE).count();

        return SeatMapResponse.builder()
                .showId(showId)
                .totalSeats(seatInfos.size())
                .availableSeats(available)
                .seats(seatInfos)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private BookingResponse toResponse(Booking b, LocalDateTime holdExpiry) {
        Show show = b.getShow();
        Screen screen = show.getScreen();

        List<BookingResponse.BookedSeatInfo> seatInfos = b.getItems().stream().map(item -> {
            Seat seat = item.getShowSeat().getSeat();
            return BookingResponse.BookedSeatInfo.builder()
                    .showSeatId(item.getShowSeat().getId())
                    .rowLabel(seat.getRowLabel())
                    .seatNumber(seat.getSeatNumber())
                    .seatType(seat.getSeatType().name())
                    .price(item.getPricePaid())
                    .build();
        }).toList();

        BigDecimal subtotal = b.getItems().stream()
                .map(BookingItem::getPricePaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String dcCode = (b.getDiscountCode() != null) ? b.getDiscountCode().getCode() : null;

        LocalDateTime expiry = holdExpiry;
        if (expiry == null && b.getStatus() == BookingStatus.PENDING && !b.getItems().isEmpty()) {
            expiry = b.getItems().get(0).getShowSeat().getHoldExpiresAt();
        }

        return BookingResponse.builder()
                .bookingId(b.getId())
                .status(b.getStatus())
                .showId(show.getId())
                .movieTitle(show.getMovie().getTitle())
                .showStartTime(show.getStartTime())
                .theaterName(screen.getTheater().getName())
                .cityName(screen.getTheater().getCity().getName())
                .seats(seatInfos)
                .subtotal(subtotal)
                .discountAmount(b.getDiscountAmount())
                .totalAmount(b.getTotalAmount())
                .discountCode(dcCode)
                .holdExpiresAt(expiry)
                .createdAt(b.getCreatedAt())
                .build();
    }
}
