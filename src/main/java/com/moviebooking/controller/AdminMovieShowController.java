package com.moviebooking.controller;

import com.moviebooking.domain.entity.Movie;
import com.moviebooking.domain.entity.Show;
import com.moviebooking.dto.request.CreateMovieRequest;
import com.moviebooking.dto.request.CreateShowRequest;
import com.moviebooking.dto.response.ApiResponse;
import com.moviebooking.dto.response.ShowResponse;
import com.moviebooking.service.MovieShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Movies & Shows")
public class AdminMovieShowController {

    private final MovieShowService movieShowService;

    // ── Movies ───────────────────────────────────────────────────────────────

    @PostMapping("/movies")
    @Operation(summary = "Add a new movie")
    public ResponseEntity<ApiResponse<Movie>> createMovie(@Valid @RequestBody CreateMovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(movieShowService.createMovie(request)));
    }

    @GetMapping("/movies")
    @Operation(summary = "List all movies")
    public ResponseEntity<ApiResponse<List<Movie>>> getMovies() {
        return ResponseEntity.ok(ApiResponse.ok(movieShowService.getAllMovies()));
    }

    // ── Shows ────────────────────────────────────────────────────────────────

    @PostMapping("/shows")
    @Operation(summary = "Schedule a new show")
    public ResponseEntity<ApiResponse<ShowResponse>> createShow(@Valid @RequestBody CreateShowRequest request) {
        Show show = movieShowService.createShow(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(movieShowService.getShowById(show.getId())));
    }

    @DeleteMapping("/shows/{id}")
    @Operation(summary = "Cancel a show")
    public ResponseEntity<ApiResponse<Void>> cancelShow(@PathVariable Long id) {
        movieShowService.cancelShow(id);
        return ResponseEntity.ok(ApiResponse.ok("Show cancelled", null));
    }
}
