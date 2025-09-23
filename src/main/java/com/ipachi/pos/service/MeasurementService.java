// src/main/java/com/ipachi/pos/service/MeasurementService.java
package com.ipachi.pos.service;

import com.ipachi.pos.dto.MeasurementCreate;
import com.ipachi.pos.dto.MeasurementDto;
import com.ipachi.pos.dto.MeasurementUpdate;
import com.ipachi.pos.model.MeasurementUnit;
import com.ipachi.pos.repo.MeasurementUnitRepository;
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
public class MeasurementService {
    private final MeasurementUnitRepository repo;
    private final CurrentRequest ctx;

    private Long biz() {
        Long v = ctx.getBusinessId();
        if (v == null) throw new IllegalStateException("Business ID not found in request headers");
        return v;
    }

    private Long user() {
        Long v = ctx.getUserId();
        if (v == null) throw new IllegalStateException("User ID not found in request headers");
        return v;
    }

    public MeasurementDto create(MeasurementCreate req) {
        Long businessId = biz();
        Long userId = user(); // stamp creator

        var name = req.name() == null ? "" : req.name().trim();
        var abbr = req.abbr() == null ? "" : req.abbr().trim();

        if (name.isBlank() || abbr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and abbreviation are required");
        }

        if (repo.existsByNameIgnoreCaseAndBusinessId(name, businessId) ||
                repo.existsByAbbrIgnoreCaseAndBusinessId(abbr, businessId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Measurement with same name or abbreviation exists");
        }

        var now = OffsetDateTime.now();
        var saved = repo.save(MeasurementUnit.builder()
                .businessId(businessId)
                .userId(userId)                // <-- ensure NOT NULL user_id
                .name(name)
                .abbr(abbr)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return toDto(saved);
    }

    public MeasurementDto update(Long id, MeasurementUpdate req) {
        Long businessId = biz();

        var unit = repo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var name = req.name() == null ? "" : req.name().trim();
        var abbr = req.abbr() == null ? "" : req.abbr().trim();
        if (name.isBlank() || abbr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and abbreviation are required");
        }

        if (repo.existsByNameIgnoreCaseAndBusinessIdAndIdNot(name, businessId, id) ||
                repo.existsByAbbrIgnoreCaseAndBusinessIdAndIdNot(abbr, businessId, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Measurement with same name or abbreviation exists");
        }

        unit.setName(name);
        unit.setAbbr(abbr);
        unit.setUpdatedAt(OffsetDateTime.now());
        return toDto(repo.save(unit));
    }

    public void delete(Long id) {
        Long businessId = biz();
        if (!repo.existsByIdAndBusinessId(id, businessId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repo.deleteById(id);
    }

    public Page<MeasurementDto> list(String q, Pageable pageable) {
        Long businessId = biz();
        var page = (q == null || q.isBlank())
                ? repo.findByBusinessId(businessId, pageable)
                : repo.findByNameContainingIgnoreCaseOrAbbrContainingIgnoreCaseAndBusinessId(q.trim(), q.trim(), businessId, pageable);
        return page.map(this::toDto);
    }

    public java.util.List<MeasurementDto> all() {
        Long businessId = biz();
        return repo.findByBusinessIdOrderByNameAsc(businessId).stream().map(this::toDto).toList();
    }

    private MeasurementDto toDto(MeasurementUnit u) {
        return new MeasurementDto(u.getId(), u.getName(), u.getAbbr(), u.getCreatedAt(), u.getUpdatedAt());
    }
}
