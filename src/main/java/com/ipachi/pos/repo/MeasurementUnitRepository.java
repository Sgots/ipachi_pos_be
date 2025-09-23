// src/main/java/com/ipachi/pos/repo/MeasurementUnitRepository.java
package com.ipachi.pos.repo;

import com.ipachi.pos.model.MeasurementUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeasurementUnitRepository extends JpaRepository<MeasurementUnit, Long> {

    @Query("SELECT COUNT(u) > 0 FROM MeasurementUnit u WHERE LOWER(u.name) = LOWER(:name) AND u.businessId = :businessId")
    boolean existsByNameIgnoreCaseAndBusinessId(@Param("name") String name, @Param("businessId") Long businessId);

    @Query("SELECT COUNT(u) > 0 FROM MeasurementUnit u WHERE LOWER(u.abbr) = LOWER(:abbr) AND u.businessId = :businessId")
    boolean existsByAbbrIgnoreCaseAndBusinessId(@Param("abbr") String abbr, @Param("businessId") Long businessId);

    @Query("SELECT COUNT(u) > 0 FROM MeasurementUnit u WHERE LOWER(u.name) = LOWER(:name) AND u.businessId = :businessId AND u.id != :id")
    boolean existsByNameIgnoreCaseAndBusinessIdAndIdNot(@Param("name") String name, @Param("businessId") Long businessId, @Param("id") Long id);

    @Query("SELECT COUNT(u) > 0 FROM MeasurementUnit u WHERE LOWER(u.abbr) = LOWER(:abbr) AND u.businessId = :businessId AND u.id != :id")
    boolean existsByAbbrIgnoreCaseAndBusinessIdAndIdNot(@Param("abbr") String abbr, @Param("businessId") Long businessId, @Param("id") Long id);

    @Query("SELECT COUNT(u) > 0 FROM MeasurementUnit u WHERE u.id = :id AND u.businessId = :businessId")
    boolean existsByIdAndBusinessId(@Param("id") Long id, @Param("businessId") Long businessId);

    @Query("SELECT u FROM MeasurementUnit u WHERE u.businessId = :businessId")
    Page<MeasurementUnit> findByBusinessId(@Param("businessId") Long businessId, Pageable pageable);

    @Query("SELECT u FROM MeasurementUnit u WHERE u.businessId = :businessId ORDER BY u.name ASC")
    List<MeasurementUnit> findByBusinessIdOrderByNameAsc(@Param("businessId") Long businessId);

    @Query("""
           SELECT u FROM MeasurementUnit u
           WHERE u.businessId = :businessId AND
           (LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))
            OR LOWER(u.abbr) LIKE LOWER(CONCAT('%', :abbr, '%')))
           """)
    Page<MeasurementUnit> findByNameContainingIgnoreCaseOrAbbrContainingIgnoreCaseAndBusinessId(
            @Param("name") String name,
            @Param("abbr") String abbr,
            @Param("businessId") Long businessId,
            Pageable pageable
    );

    Optional<MeasurementUnit> findByIdAndBusinessId(Long id, Long businessId);
}
