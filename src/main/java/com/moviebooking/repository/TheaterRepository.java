package com.moviebooking.repository;

import com.moviebooking.domain.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TheaterRepository extends JpaRepository<Theater, Long> {

    @Query("SELECT t FROM Theater t JOIN FETCH t.city WHERE t.city.id = :cityId")
    List<Theater> findByCityId(@Param("cityId") Long cityId);
}
