// src/main/java/com/ipachi/pos/service/CategoryService.java
package com.ipachi.pos.service;

import com.ipachi.pos.dto.CategoryCreate;
import com.ipachi.pos.dto.CategoryDto;
import com.ipachi.pos.dto.CategoryUpdate;
import com.ipachi.pos.model.Category;
import com.ipachi.pos.repo.CategoryRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final CurrentRequest ctx;  // read userId & businessId from headers

    private Long biz() {
        Long v = ctx.getBusinessId();
        if (v == null) throw new IllegalStateException("Business ID not found in request headers");
        return v;
    }

    private Long userId() {
        Long v = ctx.getUserId();
        if (v == null) throw new IllegalStateException("User ID not found in request headers");
        return v;
    }

    /** Create a new category for the current business */
    public CategoryDto create(CategoryCreate req) {
        Long businessId = biz();
        Long uid = userId(); // <- ensure we stamp user_id to satisfy NOT NULL

        var name = req.name() == null ? "" : req.name().trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name cannot be empty");
        }

        if (repo.existsByNameIgnoreCaseAndBusinessId(name, businessId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists for this business");
        }

        log.info("Creating category '{}' for businessId: {} by userId: {}", name, businessId, uid);
        var now = OffsetDateTime.now();

        var category = Category.builder()
                .businessId(businessId)
                .name(name)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // IMPORTANT: set the owning user to avoid SQLIntegrityConstraintViolationException on user_id
        category.setUserId(uid);

        var saved = repo.save(category);
        log.debug("Created category ID: {} for business: {} by user: {}", saved.getId(), businessId, uid);
        return toDto(saved);
    }

    /** Update an existing category (must belong to current business) */
    public CategoryDto update(Long id, CategoryUpdate req) {
        Long businessId = biz();

        var cat = repo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        var name = req.name() == null ? "" : req.name().trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name cannot be empty");
        }

        if (repo.existsByNameIgnoreCaseAndBusinessIdAndIdNot(name, businessId, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists for this business");
        }

        cat.setName(name);
        cat.setUpdatedAt(OffsetDateTime.now());
        var updated = repo.save(cat);

        log.info("Updated category ID: {} for businessId: {}", id, businessId);
        return toDto(updated);
    }

    /** Delete a category (must belong to current business) */
    public void delete(Long id) {
        Long businessId = biz();
        if (!repo.existsByIdAndBusinessId(id, businessId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        repo.deleteById(id);
        log.info("Deleted category ID: {} for businessId: {}", id, businessId);
    }

    /** List categories for the current business with optional search */
    public Page<CategoryDto> list(String q, Pageable pageable) {
        Long businessId = biz();
        Page<Category> page = (q == null || q.isBlank())
                ? repo.findByBusinessId(businessId, pageable)
                : repo.findByNameContainingIgnoreCaseAndBusinessId(q.trim(), businessId, pageable);
        return page.map(this::toDto);
    }

    /** Get all categories for the current business, sorted by name */
    public java.util.List<CategoryDto> all() {
        Long businessId = biz();
        return repo.findByBusinessIdOrderByNameAsc(businessId).stream().map(this::toDto).toList();
    }

    /** Get a single category by ID (must belong to current business) */
    public CategoryDto get(Long id) {
        Long businessId = biz();
        var category = repo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        return toDto(category);
    }

    private CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
