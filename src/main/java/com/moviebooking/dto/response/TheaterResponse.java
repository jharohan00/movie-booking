package com.moviebooking.dto.response;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class TheaterResponse {
    private Long id;
    private Long cityId;
    private String cityName;
    private String name;
    private String address;
}
