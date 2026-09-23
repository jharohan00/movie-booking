package com.moviebooking.repository;

import com.moviebooking.domain.entity.DiscountCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DiscountCode d WHERE d.code = :code")
    Optional<DiscountCode> findByCodeWithLock(@Param("code") String code);

    Optional<DiscountCode> findByCode(String code);
}
