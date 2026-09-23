package com.moviebooking.repository;

import com.moviebooking.domain.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {
    Optional<City> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
