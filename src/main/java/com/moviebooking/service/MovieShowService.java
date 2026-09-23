package com.moviebooking.service;

import com.moviebooking.domain.entity.*;
import com.moviebooking.domain.enums.ShowSeatStatus;
import com.moviebooking.dto.request.*;
import com.moviebooking.dto.response.*;
import com.moviebooking.exception.BookingException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieShowService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final ScreenRepository screenRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatRepository seatRepository;
    private final PricingService pricingService;

    @Transactional
    public Movie createMovie(CreateMovieRequest req) {
        Movie movie = Movie.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .durationMins(req.getDurationMins())
                .genre(req.getGenre())
                .language(req.getLanguage())
                .releaseDate(req.getReleaseDate())
                .build();
        return movieRepository.save(movie);
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    @Transactional
    public Show createShow(CreateShowRequest req) {
        Screen screen = screenRepository.findById(req.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found: " + req.getScreenId()));
        Movie movie = movieRepository.findById(req.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + req.getMovieId()));

        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new BookingException("End time must be after start time");
        }

        Show show = Show.builder()
                .screen(screen)
                .movie(movie)
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .build();
        show = showRepository.save(show);

        // Create ShowSeat rows for every seat in the screen
        List<Seat> seats = seatRepository.findByScreenId(screen.getId());
        List<ShowSeat> showSeats = seats.stream()
                .map(seat -> ShowSeat.builder()
                        .show(show)
                        .seat(seat)
                        .status(ShowSeatStatus.AVAILABLE)
                        .build())
                .toList();
        showSeatRepository.saveAll(showSeats);

        return show;
    }

    @Transactional
    public void cancelShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
        show.setStatus(com.moviebooking.domain.enums.ShowStatus.CANCELLED);
        showRepository.save(show);
    }

    public List<ShowResponse> findShows(Long cityId, Long movieId, LocalDate date) {
        LocalDateTime from = (date != null) ? date.atStartOfDay() : LocalDateTime.now();
        LocalDateTime to   = (date != null) ? date.plusDays(1).atStartOfDay() : LocalDateTime.now().plusDays(30);
        return showRepository.findShows(cityId, movieId, from, to)
                .stream()
                .map(s -> pricingService.toShowResponse(s))
                .toList();
    }

    public ShowResponse getShowById(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found: " + showId));
        return pricingService.toShowResponse(show);
    }
}
