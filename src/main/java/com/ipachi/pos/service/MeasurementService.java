package com.ipachi.pos.service;

// src/main/java/com/ipachi/pos/inventory/service/MeasurementService.java

import com.ipachi.pos.dto.MeasurementCreate;
import com.ipachi.pos.dto.MeasurementDto;
import com.ipachi.pos.dto.MeasurementUpdate;
import com.ipachi.pos.model.MeasurementUnit;
import com.ipachi.pos.repo.MeasurementUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor
public class MeasurementService {
    private final MeasurementUnitRepository repo;

    public MeasurementDto create(MeasurementCreate req) {
        var name = req.name().trim();
        var abbr = req.abbr().trim();
        if (repo.existsByNameIgnoreCase(name) || repo.existsByAbbrIgnoreCase(abbr))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Measurement with same name or abbreviation exists");
        var saved = repo.save(MeasurementUnit.builder().name(name).abbr(abbr).build());
        return toDto(saved);
    }

    public MeasurementDto update(Long id, MeasurementUpdate req) {
        var unit = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var name = req.name().trim();
        var abbr = req.abbr().trim();
        if (repo.existsByNameIgnoreCaseAndIdNot(name, id) || repo.existsByAbbrIgnoreCaseAndIdNot(abbr, id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Measurement with same name or abbreviation exists");
        unit.setName(name);
        unit.setAbbr(abbr);
        return toDto(repo.save(unit));
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        // TODO: enforce referential rules if products reference this unit
        repo.deleteById(id);
    }

    public Page<MeasurementDto> list(String q, Pageable pageable) {
        var page = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNameContainingIgnoreCaseOrAbbrContainingIgnoreCase(q.trim(), q.trim(), pageable);
        return page.map(this::toDto);
    }

    public java.util.List<MeasurementDto> all() {
        return repo.findAll(Sort.by("name").ascending()).stream().map(this::toDto).toList();
    }

    private MeasurementDto toDto(MeasurementUnit u) {
        return new MeasurementDto(u.getId(), u.getName(), u.getAbbr(), u.getCreatedAt(), u.getUpdatedAt());
    }
}
