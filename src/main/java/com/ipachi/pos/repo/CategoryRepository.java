// src/main/java/com/ipachi/pos/repo/CategoryRepository.java
package com.ipachi.pos.repo;

import com.ipachi.pos.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndBusinessId(Long id, Long businessId);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.businessId = :businessId")
    boolean existsByNameIgnoreCaseAndBusinessId(@Param("name") String name, @Param("businessId") Long businessId);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.businessId = :businessId AND c.id != :id")
    boolean existsByNameIgnoreCaseAndBusinessIdAndIdNot(@Param("name") String name, @Param("businessId") Long businessId, @Param("id") Long id);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.id = :id AND c.businessId = :businessId")
    boolean existsByIdAndBusinessId(@Param("id") Long id, @Param("businessId") Long businessId);

    List<Category> findByBusinessIdOrderByNameAsc(Long businessId);

    Page<Category> findByBusinessId(Long businessId, Pageable pageable);

    Page<Category> findByNameContainingIgnoreCaseAndBusinessId(String name, Long businessId, Pageable pageable);
}
