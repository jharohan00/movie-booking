package com.moviebooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateShowRequest {
    @NotNull @Positive
    private Long screenId;

    @NotNull @Positive
    private Long movieId;

    @NotNull @Future
    private LocalDateTime startTime;

    @NotNull @Future
    private LocalDateTime endTime;
}
