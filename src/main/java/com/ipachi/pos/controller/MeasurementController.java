package com.ipachi.pos.controller;

// src/main/java/com/ipachi/pos/inventory/controller/MeasurementController.java

import com.ipachi.pos.dto.MeasurementCreate;
import com.ipachi.pos.dto.MeasurementDto;
import com.ipachi.pos.dto.MeasurementUpdate;
import com.ipachi.pos.service.MeasurementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/measurements")
@RequiredArgsConstructor
public class MeasurementController {

    private final MeasurementService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeasurementDto create(@RequestBody @Valid MeasurementCreate req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public MeasurementDto update(@PathVariable Long id, @RequestBody @Valid MeasurementUpdate req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping
    public Page<MeasurementDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        var sortObj = Sort.by(sort.split(",")[0]).ascending();
        if (sort.toLowerCase().endsWith(",desc")) sortObj = sortObj.descending();
        return service.list(q, PageRequest.of(page, size, sortObj));
    }

    @GetMapping("/all")
    public java.util.List<MeasurementDto> all() {
        return service.all();
    }
}
