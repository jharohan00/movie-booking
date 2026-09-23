package com.moviebooking.repository;

import com.moviebooking.domain.entity.PricingTier;
import com.moviebooking.domain.enums.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PricingTierRepository extends JpaRepository<PricingTier, Long> {
    List<PricingTier> findByShowId(Long showId);
    Optional<PricingTier> findByShowIdAndSeatType(Long showId, SeatType seatType);
}
