package com.moviebooking.dto.response;

import com.moviebooking.domain.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class BookingResponse {
    private Long bookingId;
    private BookingStatus status;
    private Long showId;
    private String movieTitle;
    private LocalDateTime showStartTime;
    private String theaterName;
    private String cityName;
    private List<BookedSeatInfo> seats;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String discountCode;
    private LocalDateTime holdExpiresAt;  // present while PENDING
    private LocalDateTime createdAt;

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class BookedSeatInfo {
        private Long showSeatId;
        private String rowLabel;
        private Integer seatNumber;
        private String seatType;
        private BigDecimal price;
    }
}
