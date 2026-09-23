package com.moviebooking.dto.response;

import com.moviebooking.domain.enums.SeatType;
import com.moviebooking.domain.enums.ShowSeatStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class SeatMapResponse {
    private Long showId;
    private int totalSeats;
    private long availableSeats;
    private List<SeatInfo> seats;

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class SeatInfo {
        private Long showSeatId;
        private Long seatId;
        private String rowLabel;
        private Integer seatNumber;
        private SeatType seatType;
        private ShowSeatStatus status;
        private BigDecimal price;
    }
}
