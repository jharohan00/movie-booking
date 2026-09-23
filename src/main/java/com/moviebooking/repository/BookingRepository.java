package com.moviebooking.repository;

import com.moviebooking.domain.entity.Booking;
import com.moviebooking.domain.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
           SELECT b FROM Booking b
           JOIN FETCH b.show s
           JOIN FETCH s.movie
           JOIN FETCH b.items i
           JOIN FETCH i.showSeat ss
           JOIN FETCH ss.seat
           WHERE b.id = :id
           """)
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    @Query("""
           SELECT b FROM Booking b
           JOIN FETCH b.show s
           JOIN FETCH s.movie
           WHERE b.user.id = :userId
           ORDER BY b.createdAt DESC
           """)
    List<Booking> findByUserId(@Param("userId") Long userId);

    @Query("""
           SELECT b FROM Booking b
           JOIN FETCH b.show s
           JOIN FETCH s.movie
           JOIN FETCH b.items i
           JOIN FETCH i.showSeat ss
           WHERE b.show.id = :showId
             AND b.status = :status
           """)
    List<Booking> findByShowIdAndStatus(@Param("showId") Long showId, @Param("status") BookingStatus status);
}
