package com.moviebooking.dto.request;

import com.moviebooking.domain.enums.SeatType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateScreenRequest {
    @NotBlank
    private String name;

    /** Total rows in the screen. Rows are labelled A, B, C... */
    @NotNull @Positive
    private Integer totalRows;

    /** Number of seats per row. */
    @NotNull @Positive
    private Integer seatsPerRow;

    /**
     * First N rows (1-indexed) from the front are PREMIUM, the rest are REGULAR.
     * Set to 0 for all-REGULAR layout.
     */
    @NotNull @Min(0)
    private Integer premiumRows;
}
