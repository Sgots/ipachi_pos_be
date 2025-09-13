package com.ipachi.pos.repo;

// src/main/java/com/ipachi/pos/inventory/repo/CategoryRepository.java

import com.ipachi.pos.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    Page<Category> findByNameContainingIgnoreCase(String q, Pageable pageable);
}
