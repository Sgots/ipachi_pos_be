package com.ipachi.pos.repo;

import com.ipachi.pos.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByBusinessIdOrderByNameAsc(Long businessId);

    Optional<Location> findByIdAndBusinessId(Long id, Long businessId);

    boolean existsByBusinessIdAndNameIgnoreCase(Long businessId, String name);

    boolean existsByBusinessIdAndNameIgnoreCaseAndIdNot(Long businessId, String name, Long id);

    boolean existsByIdAndBusinessId(Long id, Long businessId);
}
