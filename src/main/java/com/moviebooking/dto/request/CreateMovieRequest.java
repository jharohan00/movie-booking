package com.moviebooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateMovieRequest {
    @NotBlank
    private String title;

    private String description;

    @Positive
    private Integer durationMins;

    private String genre;

    private String language;

    private LocalDate releaseDate;
}
