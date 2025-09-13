package com.ipachi.pos.controller;

// src/main/java/com/ipachi/pos/inventory/controller/CategoryController.java

import com.ipachi.pos.dto.CategoryCreate;
import com.ipachi.pos.dto.CategoryDto;
import com.ipachi.pos.dto.CategoryUpdate;
import com.ipachi.pos.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@RequestBody @Valid CategoryCreate req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable Long id, @RequestBody @Valid CategoryUpdate req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping
    public Page<CategoryDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        var sortObj = Sort.by(sort.split(",")[0]).ascending();
        if (sort.toLowerCase().endsWith(",desc")) sortObj = sortObj.descending();
        return service.list(q, PageRequest.of(page, size, sortObj));
    }

    // lightweight list for dropdowns
    @GetMapping("/all")
    public java.util.List<CategoryDto> all() {
        return service.all();
    }
}

