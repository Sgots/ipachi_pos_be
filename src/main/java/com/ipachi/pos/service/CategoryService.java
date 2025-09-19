package com.ipachi.pos.service;

import com.ipachi.pos.dto.CategoryCreate;
import com.ipachi.pos.dto.CategoryDto;
import com.ipachi.pos.dto.CategoryUpdate;
import com.ipachi.pos.model.Category;
import com.ipachi.pos.repo.CategoryRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {
    private final CategoryRepository repo;
    private final CurrentRequest ctx;  // Inject CurrentRequest for userId from headers

    /**
     * Create a new category for the current user
     */
    public CategoryDto create(CategoryCreate req) {
        // Validate user context from headers
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }

        var name = req.name().trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name cannot be empty");
        }

        if (repo.existsByNameIgnoreCaseAndUserId(name, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists for this user");
        }

        log.info("Creating category '{}' for userId: {}", name, userId);

        // Use builder pattern with userId from context
        var category = Category.builder()
                .name(name)
                .userId(userId)  // Set userId from header context
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        var saved = repo.save(category);
        log.debug("Created category ID: {} for user: {}", saved.getId(), userId);

        return toDto(saved);
    }

    /**
     * Update an existing category (must belong to current user)
     */
    public CategoryDto update(Long id, CategoryUpdate req) {
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }

        var cat = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        var name = req.name().trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name cannot be empty");
        }

        if (repo.existsByNameIgnoreCaseAndUserIdAndIdNot(name, userId, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists for this user");
        }

        log.info("Updating category ID: {} for userId: {}", id, userId);
        cat.setName(name);
        cat.setUpdatedAt(OffsetDateTime.now());

        var updated = repo.save(cat);
        log.debug("Updated category ID: {} for user: {}", updated.getId(), userId);

        return toDto(updated);
    }

    /**
     * Delete a category (must belong to current user)
     */
    public void delete(Long id) {
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }

        if (!repo.existsByIdAndUserId(id, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }

        // TODO: if products reference this, either forbid or cascade (choose your rule)
        log.info("Deleting category ID: {} for userId: {}", id, userId);
        repo.deleteById(id);
        log.debug("Deleted category ID: {} for user: {}", id, userId);
    }

    /**
     * List categories for the current user with optional search
     */
    public Page<CategoryDto> list(String q, Pageable pageable) {
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }

        log.debug("Listing categories for userId: {}, query: {}, page: {}", userId, q, pageable);

        Page<Category> page;
        if (q == null || q.isBlank()) {
            page = repo.findByUserId(userId, pageable);
        } else {
            page = repo.findByNameContainingIgnoreCaseAndUserId(q.trim(), userId, pageable);
        }

        var result = page.map(this::toDto);
        log.debug("Found {} categories for user: {}", result.getTotalElements(), userId);

        return result;
    }

    /**
     * Get all categories for the current user, sorted by name
     */
    public java.util.List<CategoryDto> all() {
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }

        log.debug("Getting all categories for userId: {}", userId);
        var categories = repo.findByUserIdOrderByNameAsc(userId);
        var result = categories.stream().map(this::toDto).toList();
        log.debug("Found {} categories for user: {}", result.size(), userId);

        return result;
    }

    /**
     * Get a single category by ID (must belong to current user)
     */
    public CategoryDto get(Long id) {
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }

        var category = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        log.debug("Retrieved category ID: {} for user: {}", id, userId);
        return toDto(category);
    }

    private CategoryDto toDto(Category c) {
        return new CategoryDto(
                c.getId(),
                c.getName(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}