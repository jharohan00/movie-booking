package com.moviebooking.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class ShowResponse {
    private Long id;
    private String movieTitle;
    private Integer durationMins;
    private String genre;
    private String language;
    private String theaterName;
    private String screenName;
    private String cityName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private long availableSeats;
    private List<PricingTierInfo> pricingTiers;

    @Data @AllArgsConstructor @NoArgsConstructor @Builder
    public static class PricingTierInfo {
        private String seatType;
        private java.math.BigDecimal basePrice;
        private java.math.BigDecimal weekendPrice;
        private boolean weekendPricingEnabled;
    }
}
