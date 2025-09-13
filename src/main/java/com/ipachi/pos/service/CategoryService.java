package com.ipachi.pos.service;

// src/main/java/com/ipachi/pos/inventory/service/CategoryService.java

import com.ipachi.pos.dto.CategoryCreate;
import com.ipachi.pos.dto.CategoryDto;
import com.ipachi.pos.dto.CategoryUpdate;
import com.ipachi.pos.model.Category;
import com.ipachi.pos.repo.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository repo;

    public CategoryDto create(CategoryCreate req) {
        var name = req.name().trim();
        if (repo.existsByNameIgnoreCase(name))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        var saved = repo.save(Category.builder().name(name).build());
        return toDto(saved);
    }

    public CategoryDto update(Long id, CategoryUpdate req) {
        var cat = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var name = req.name().trim();
        if (repo.existsByNameIgnoreCaseAndIdNot(name, id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        cat.setName(name);
        return toDto(repo.save(cat));
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        // TODO: if products reference this, either forbid or cascade (choose your rule)
        repo.deleteById(id);
    }

    public Page<CategoryDto> list(String q, Pageable pageable) {
        var page = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNameContainingIgnoreCase(q.trim(), pageable);
        return page.map(this::toDto);
    }

    public java.util.List<CategoryDto> all() {
        return repo.findAll(Sort.by("name").ascending()).stream().map(this::toDto).toList();
    }

    private CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
