package com.moviebooking.repository;

import com.moviebooking.domain.entity.Show;
import com.moviebooking.domain.enums.ShowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("""
           SELECT s FROM Show s
           JOIN FETCH s.movie m
           JOIN FETCH s.screen sc
           JOIN FETCH sc.theater t
           JOIN FETCH t.city c
           WHERE s.status = 'SCHEDULED'
             AND (:cityId IS NULL OR c.id = :cityId)
             AND (:movieId IS NULL OR m.id = :movieId)
             AND s.startTime >= :from
             AND s.startTime < :to
           ORDER BY s.startTime
           """)
    List<Show> findShows(@Param("cityId") Long cityId,
                         @Param("movieId") Long movieId,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to);

    @Query("""
           SELECT s FROM Show s
           JOIN FETCH s.movie
           JOIN FETCH s.screen sc
           JOIN FETCH sc.theater t
           JOIN FETCH t.city
           WHERE s.status = 'SCHEDULED'
             AND s.startTime >= :from
             AND s.startTime < :to
           """)
    List<Show> findUpcomingShows(@Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);
}
