package com.ipachi.pos.service;

import com.ipachi.pos.dto.MeasurementCreate;
import com.ipachi.pos.dto.MeasurementDto;
import com.ipachi.pos.dto.MeasurementUpdate;
import com.ipachi.pos.model.MeasurementUnit;
import com.ipachi.pos.repo.MeasurementUnitRepository;
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
public class MeasurementService {
    private final MeasurementUnitRepository repo;
    private final CurrentRequest ctx;  // Added CurrentRequest injection

    // Helper method to validate user context (minimal addition)
    private Long validateUserContext() {
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }
        return userId;
    }

    public MeasurementDto create(MeasurementCreate req) {
        Long userId = validateUserContext();  // Added user validation

        var name = req.name().trim();
        var abbr = req.abbr().trim();
        // Updated to check uniqueness within user scope
        if (repo.existsByNameIgnoreCaseAndUserId(name, userId) || repo.existsByAbbrIgnoreCaseAndUserId(abbr, userId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Measurement with same name or abbreviation exists");

        // Added userId and timestamps to builder
        var saved = repo.save(MeasurementUnit.builder()
                .name(name)
                .abbr(abbr)
                .userId(userId)  // Added userId
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build());
        return toDto(saved);
    }

    public MeasurementDto update(Long id, MeasurementUpdate req) {
        Long userId = validateUserContext();  // Added user validation

        // Updated to find by ID and userId
        var unit = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var name = req.name().trim();
        var abbr = req.abbr().trim();
        // Updated to check uniqueness within user scope, excluding current ID
        if (repo.existsByNameIgnoreCaseAndUserIdAndIdNot(name, userId, id) ||
                repo.existsByAbbrIgnoreCaseAndUserIdAndIdNot(abbr, userId, id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Measurement with same name or abbreviation exists");

        unit.setName(name);
        unit.setAbbr(abbr);
        unit.setUpdatedAt(OffsetDateTime.now());  // Added timestamp update
        return toDto(repo.save(unit));
    }

    public void delete(Long id) {
        Long userId = validateUserContext();  // Added user validation

        // Updated to check existence with userId
        if (!repo.existsByIdAndUserId(id, userId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        // TODO: enforce referential rules if products reference this unit
        repo.deleteById(id);
    }

    public Page<MeasurementDto> list(String q, Pageable pageable) {
        Long userId = validateUserContext();  // Added user validation

        // Updated to filter by userId
        var page = (q == null || q.isBlank())
                ? repo.findByUserId(userId, pageable)
                : repo.findByNameContainingIgnoreCaseOrAbbrContainingIgnoreCaseAndUserId(q.trim(), q.trim(), userId, pageable);
        return page.map(this::toDto);
    }

    public java.util.List<MeasurementDto> all() {
        Long userId = validateUserContext();  // Added user validation

        // Updated to filter by userId and sort
        return repo.findByUserIdOrderByNameAsc(userId).stream().map(this::toDto).toList();
    }

    private MeasurementDto toDto(MeasurementUnit u) {
        return new MeasurementDto(u.getId(), u.getName(), u.getAbbr(), u.getCreatedAt(), u.getUpdatedAt());
    }
}