package com.moviebooking.controller;

import com.moviebooking.dto.response.ApiResponse;
import com.moviebooking.dto.response.SeatMapResponse;
import com.moviebooking.dto.response.ShowResponse;
import com.moviebooking.service.BookingService;
import com.moviebooking.service.MovieShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
@Tag(name = "Shows — Browse")
public class CustomerShowController {

    private final MovieShowService movieShowService;
    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "Browse shows (filter by cityId, movieId, date)")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShows(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(movieShowService.findShows(cityId, movieId, date)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get show details")
    public ResponseEntity<ApiResponse<ShowResponse>> getShow(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(movieShowService.getShowById(id)));
    }

    @GetMapping("/{id}/seats")
    @Operation(summary = "Get seat map with live availability and prices")
    public ResponseEntity<ApiResponse<SeatMapResponse>> getSeatMap(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getSeatMap(id)));
    }
}
