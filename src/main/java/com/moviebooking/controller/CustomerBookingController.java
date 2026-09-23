package com.moviebooking.controller;

import com.moviebooking.dto.request.HoldSeatsRequest;
import com.moviebooking.dto.response.ApiResponse;
import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
@Tag(name = "Bookings")
public class CustomerBookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Hold seats — initiates booking (Phase 1)")
    public ResponseEntity<ApiResponse<BookingResponse>> holdSeats(@Valid @RequestBody HoldSeatsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Seats held successfully. You have 10 minutes to confirm.",
                        bookingService.holdSeats(request)));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm booking and process payment (Phase 2)")
    public ResponseEntity<ApiResponse<BookingResponse>> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Booking confirmed!", bookingService.confirmBooking(id)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getBooking(id)));
    }

    @GetMapping
    @Operation(summary = "Get my booking history")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> myBookings() {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getMyBookings()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled", bookingService.cancelBooking(id)));
    }
}
