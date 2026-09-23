package com.moviebooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateTheaterRequest {
    @NotNull @Positive
    private Long cityId;

    @NotBlank
    private String name;

    private String address;
}
