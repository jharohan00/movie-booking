package com.moviebooking.dto.request;

import com.moviebooking.domain.enums.SeatType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SetPricingRequest {

    @NotNull @Valid
    private List<TierRequest> tiers;

    @Data
    public static class TierRequest {
        @NotNull
        private SeatType seatType;

        @NotNull @DecimalMin("0.01")
        private BigDecimal basePrice;

        private boolean weekendPricingEnabled = false;

        @DecimalMin("1.0")
        private BigDecimal weekendMultiplier = BigDecimal.ONE;
    }
}
