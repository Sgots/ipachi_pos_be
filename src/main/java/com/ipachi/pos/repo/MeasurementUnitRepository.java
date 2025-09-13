package com.ipachi.pos.repo;

// src/main/java/com/ipachi/pos/inventory/repo/MeasurementUnitRepository.java

import com.ipachi.pos.model.MeasurementUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeasurementUnitRepository extends JpaRepository<MeasurementUnit, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByAbbrIgnoreCase(String abbr);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByAbbrIgnoreCaseAndIdNot(String abbr, Long id);

    Page<MeasurementUnit> findByNameContainingIgnoreCaseOrAbbrContainingIgnoreCase(
            String name, String abbr, Pageable pageable);
}

