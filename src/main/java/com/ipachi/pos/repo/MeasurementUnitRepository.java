package com.ipachi.pos.repo;

// src/main/java/com/ipachi/pos/inventory/repo/MeasurementUnitRepository.java

import com.ipachi.pos.model.MeasurementUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeasurementUnitRepository extends JpaRepository<MeasurementUnit, Long> {
    // Add these methods to MeasurementUnitRepository
    @Query("SELECT COUNT(u) > 0 FROM MeasurementUnit u WHERE LOWER(u.name) = LOWER(:name) AND u.userId = :userId")
    boolean existsByNameIgnoreCaseAndUserId(@Param("name") String name, @Param("userId") Long userId);

    @Query("SELECT COUNT(u) > 0 FROM MeasurementUnit u WHERE LOWER(u.abbr) = LOWER(:abbr) AND u.userId = :userId")
    boolean existsByAbbrIgnoreCaseAndUserId(@Param("abbr") String abbr, @Param("userId") Long userId);

    @Query("SELECT COUNT(u) > 0 FROM MeasurementUnit u WHERE LOWER(u.name) = LOWER(:name) AND u.userId = :userId AND u.id != :id")
    boolean existsByNameIgnoreCaseAndUserIdAndIdNot(@Param("name") String name, @Param("userId") Long userId, @Param("id") Long id);

    @Query("SELECT COUNT(u) > 0 FROM MeasurementUnit u WHERE LOWER(u.abbr) = LOWER(:abbr) AND u.userId = :userId AND u.id != :id")
    boolean existsByAbbrIgnoreCaseAndUserIdAndIdNot(@Param("abbr") String abbr, @Param("userId") Long userId, @Param("id") Long id);


    @Query("SELECT COUNT(u) > 0 FROM MeasurementUnit u WHERE u.id = :id AND u.userId = :userId")
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT u FROM MeasurementUnit u WHERE u.userId = :userId")
    Page<MeasurementUnit> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT u FROM MeasurementUnit u WHERE u.userId = :userId ORDER BY u.name ASC")
    List<MeasurementUnit> findByUserIdOrderByNameAsc(@Param("userId") Long userId);

    @Query("SELECT u FROM MeasurementUnit u WHERE u.userId = :userId AND " +
            "(LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(u.abbr) LIKE LOWER(CONCAT('%', :abbr, '%')))")
    Page<MeasurementUnit> findByNameContainingIgnoreCaseOrAbbrContainingIgnoreCaseAndUserId(
            @Param("name") String name,
            @Param("abbr") String abbr,
            @Param("userId") Long userId,
            Pageable pageable
    );
    boolean existsByNameIgnoreCase(String name);
    boolean existsByAbbrIgnoreCase(String abbr);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByAbbrIgnoreCaseAndIdNot(String abbr, Long id);
    List<MeasurementUnit> findByUserId(Long userId);
    Optional<MeasurementUnit> findByIdAndUserId(Long id, Long userId);
    Page<MeasurementUnit> findByNameContainingIgnoreCaseOrAbbrContainingIgnoreCase(
            String name, String abbr, Pageable pageable);
}

