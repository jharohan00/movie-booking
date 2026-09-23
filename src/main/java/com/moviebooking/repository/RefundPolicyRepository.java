package com.moviebooking.repository;

import com.moviebooking.domain.entity.RefundPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {

    /** Show-level override — highest priority. */
    @Query("SELECT r FROM RefundPolicy r WHERE r.show.id = :showId ORDER BY r.hoursBeforeShow DESC")
    List<RefundPolicy> findByShowId(@Param("showId") Long showId);

    /** Theater-level policy. */
    @Query("SELECT r FROM RefundPolicy r WHERE r.theater.id = :theaterId AND r.show IS NULL ORDER BY r.hoursBeforeShow DESC")
    List<RefundPolicy> findByTheaterId(@Param("theaterId") Long theaterId);

    /** Global default (no theater, no show). */
    @Query("SELECT r FROM RefundPolicy r WHERE r.theater IS NULL AND r.show IS NULL ORDER BY r.hoursBeforeShow DESC")
    List<RefundPolicy> findGlobalPolicies();
}
