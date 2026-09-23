package com.moviebooking.service;

import com.moviebooking.domain.entity.*;
import com.moviebooking.domain.enums.SeatType;
import com.moviebooking.dto.request.*;
import com.moviebooking.dto.response.*;
import com.moviebooking.exception.BookingException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CityTheaterService {

    private final CityRepository cityRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public CityResponse createCity(CreateCityRequest request) {
        if (cityRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BookingException("City already exists: " + request.getName());
        }
        City city = City.builder()
                .name(request.getName())
                .state(request.getState())
                .build();
        city = cityRepository.save(city);
        return toDto(city);
    }

    public List<CityResponse> getAllCities() {
        return cityRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public TheaterResponse createTheater(CreateTheaterRequest request) {
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + request.getCityId()));
        Theater theater = Theater.builder()
                .city(city)
                .name(request.getName())
                .address(request.getAddress())
                .build();
        theater = theaterRepository.save(theater);
        return toDto(theater);
    }

    @Transactional
    public TheaterResponse updateTheater(Long id, CreateTheaterRequest request) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found: " + id));
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + request.getCityId()));
        theater.setCity(city);
        theater.setName(request.getName());
        theater.setAddress(request.getAddress());
        return toDto(theaterRepository.save(theater));
    }

    public List<TheaterResponse> getTheatersByCity(Long cityId) {
        return theaterRepository.findByCityId(cityId).stream().map(this::toDto).toList();
    }

    @Transactional
    public ScreenResponse createScreen(Long theaterId, CreateScreenRequest request) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found: " + theaterId));

        Screen screen = Screen.builder()
                .theater(theater)
                .name(request.getName())
                .build();
        screen = screenRepository.save(screen);

        // Generate seat layout
        List<Seat> seats = new ArrayList<>();
        for (int r = 0; r < request.getTotalRows(); r++) {
            String rowLabel = String.valueOf((char) ('A' + r));
            SeatType type = (r < request.getPremiumRows()) ? SeatType.PREMIUM : SeatType.REGULAR;
            for (int s = 1; s <= request.getSeatsPerRow(); s++) {
                seats.add(Seat.builder()
                        .screen(screen)
                        .rowLabel(rowLabel)
                        .seatNumber(s)
                        .seatType(type)
                        .build());
            }
        }
        seatRepository.saveAll(seats);

        return ScreenResponse.builder()
                .id(screen.getId())
                .theaterId(theaterId)
                .name(screen.getName())
                .totalSeats(seats.size())
                .build();
    }

    private CityResponse toDto(City c) {
        return CityResponse.builder().id(c.getId()).name(c.getName()).state(c.getState()).build();
    }

    private TheaterResponse toDto(Theater t) {
        return TheaterResponse.builder()
                .id(t.getId())
                .cityId(t.getCity().getId())
                .cityName(t.getCity().getName())
                .name(t.getName())
                .address(t.getAddress())
                .build();
    }
}
