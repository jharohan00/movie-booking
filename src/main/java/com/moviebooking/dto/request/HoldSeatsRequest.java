package com.moviebooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class HoldSeatsRequest {
    @NotNull @Positive
    private Long showId;

    @NotNull @Size(min = 1, max = 10, message = "Select between 1 and 10 seats")
    private List<Long> showSeatIds;

    /** Optional discount code to apply at confirmation. */
    private String discountCode;
}
