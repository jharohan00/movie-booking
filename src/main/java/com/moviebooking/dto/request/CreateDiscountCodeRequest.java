package com.moviebooking.dto.request;

import com.moviebooking.domain.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateDiscountCodeRequest {
    @NotBlank @Size(max = 50)
    private String code;

    @NotNull
    private DiscountType discountType;

    @NotNull @DecimalMin("0.01")
    private BigDecimal value;

    @Positive
    private Integer maxUses;  // null = unlimited

    private Long showId;  // null = global

    @NotNull
    private LocalDateTime validFrom;

    @NotNull
    private LocalDateTime validTo;
}
