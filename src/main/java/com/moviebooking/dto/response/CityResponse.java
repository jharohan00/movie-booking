package com.moviebooking.dto.response;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class CityResponse {
    private Long id;
    private String name;
    private String state;
}
