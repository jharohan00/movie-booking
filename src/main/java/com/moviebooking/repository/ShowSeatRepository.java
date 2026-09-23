package com.moviebooking.repository;

import com.moviebooking.domain.entity.ShowSeat;
import com.moviebooking.domain.enums.ShowSeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    @Query("SELECT ss FROM ShowSeat ss JOIN FETCH ss.seat WHERE ss.show.id = :showId ORDER BY ss.seat.rowLabel, ss.seat.seatNumber")
    List<ShowSeat> findByShowId(@Param("showId") Long showId);

    /**
     * Acquires a pessimistic write lock on the requested seats to prevent
     * concurrent allocation. Rows are ordered by ID to avoid deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowSeat ss JOIN FETCH ss.seat WHERE ss.id IN :ids ORDER BY ss.id")
    List<ShowSeat> findByIdsWithLock(@Param("ids") List<Long> ids);

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.seat.id IN :seatIds")
    List<ShowSeat> findByShowIdAndSeatIds(@Param("showId") Long showId, @Param("seatIds") List<Long> seatIds);

    /**
     * Expire held seats whose TTL has passed — used by the sweeper.
     * Returns the list first, then the update is done separately to avoid
     * the SELECT FOR UPDATE vs UPDATE conflict in Postgres with JPQL.
     */
    @Query("""
           SELECT ss FROM ShowSeat ss
           WHERE ss.status = 'HELD'
             AND ss.holdExpiresAt < :now
           """)
    List<ShowSeat> findExpiredHolds(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(ss) FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.status = :status")
    long countByShowIdAndStatus(@Param("showId") Long showId, @Param("status") ShowSeatStatus status);
}
