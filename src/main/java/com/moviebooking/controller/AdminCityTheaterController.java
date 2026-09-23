package com.moviebooking.controller;

import com.moviebooking.dto.request.*;
import com.moviebooking.dto.response.*;
import com.moviebooking.service.CityTheaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Cities & Theaters")
public class AdminCityTheaterController {

    private final CityTheaterService cityTheaterService;

    // ── Cities ──────────────────────────────────────────────────────────────

    @PostMapping("/cities")
    @Operation(summary = "Create a city")
    public ResponseEntity<ApiResponse<CityResponse>> createCity(@Valid @RequestBody CreateCityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(cityTheaterService.createCity(request)));
    }

    @GetMapping("/cities")
    @Operation(summary = "List all cities")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getCities() {
        return ResponseEntity.ok(ApiResponse.ok(cityTheaterService.getAllCities()));
    }

    // ── Theaters ─────────────────────────────────────────────────────────────

    @PostMapping("/theaters")
    @Operation(summary = "Create a theater in a city")
    public ResponseEntity<ApiResponse<TheaterResponse>> createTheater(@Valid @RequestBody CreateTheaterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(cityTheaterService.createTheater(request)));
    }

    @PutMapping("/theaters/{id}")
    @Operation(summary = "Update a theater")
    public ResponseEntity<ApiResponse<TheaterResponse>> updateTheater(@PathVariable Long id,
                                                                      @Valid @RequestBody CreateTheaterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(cityTheaterService.updateTheater(id, request)));
    }

    @GetMapping("/theaters")
    @Operation(summary = "List theaters by city")
    public ResponseEntity<ApiResponse<List<TheaterResponse>>> getTheaters(@RequestParam Long cityId) {
        return ResponseEntity.ok(ApiResponse.ok(cityTheaterService.getTheatersByCity(cityId)));
    }

    // ── Screens ──────────────────────────────────────────────────────────────

    @PostMapping("/theaters/{theaterId}/screens")
    @Operation(summary = "Add a screen with seat layout to a theater")
    public ResponseEntity<ApiResponse<ScreenResponse>> createScreen(@PathVariable Long theaterId,
                                                                    @Valid @RequestBody CreateScreenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(cityTheaterService.createScreen(theaterId, request)));
    }
}
