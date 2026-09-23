package com.moviebooking.dto.response;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class ScreenResponse {
    private Long id;
    private Long theaterId;
    private String name;
    private int totalSeats;
}
