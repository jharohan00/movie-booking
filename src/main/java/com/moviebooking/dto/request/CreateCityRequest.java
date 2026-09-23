package com.moviebooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCityRequest {
    @NotBlank @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String state;
}
