package com.moviebooking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateRefundPolicyRequest {
    /** Null = global default policy. */
    private Long theaterId;

    /** Null = applies to all shows in theater (or globally). Non-null = show override. */
    private Long showId;

    @NotNull @Valid
    private List<BracketRequest> brackets;

    @Data
    public static class BracketRequest {
        /** Hours before show; cancellation at this threshold or later qualifies. */
        @NotNull @Min(0)
        private Integer hoursBeforeShow;

        /** Percentage of ticket price refunded (0–100). */
        @NotNull @DecimalMin("0") @DecimalMax("100")
        private BigDecimal refundPercent;
    }
}
