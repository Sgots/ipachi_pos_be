package com.ipachi.pos.repo;

import com.ipachi.pos.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Using Spring Data JPA naming convention - this should work automatically
    Optional<Category> findByIdAndUserId(Long id, Long userId);

    // Check if category name exists for a specific user
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.userId = :userId")
    boolean existsByNameIgnoreCaseAndUserId(@Param("name") String name, @Param("userId") Long userId);

    // Check if category name exists for a specific user, excluding current ID
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.userId = :userId AND c.id != :id")
    boolean existsByNameIgnoreCaseAndUserIdAndIdNot(@Param("name") String name, @Param("userId") Long userId, @Param("id") Long id);

    // Check if category exists by ID and userId
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.id = :id AND c.userId = :userId")
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    // Find all categories for a user
    List<Category> findByUserIdOrderByNameAsc(Long userId);

    // Find categories for a user with pagination
    Page<Category> findByUserId(Long userId, Pageable pageable);

    // Search categories for a user with pagination
    Page<Category> findByNameContainingIgnoreCaseAndUserId(String name, Long userId, Pageable pageable);

}