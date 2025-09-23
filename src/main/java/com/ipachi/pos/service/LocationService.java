package com.ipachi.pos.service;

import com.ipachi.pos.dto.LocationCreate;
import com.ipachi.pos.dto.LocationDto;
import com.ipachi.pos.dto.LocationUpdate;
import com.ipachi.pos.model.Location;
import com.ipachi.pos.repo.LocationRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationService {

    private final LocationRepository repo;
    private final CurrentRequest ctx;

    private Long biz() {
        Long v = ctx.getBusinessId();
        if (v == null) throw new IllegalStateException("Business ID not found in request headers");
        return v;
    }

    public List<LocationDto> list() {
        Long businessId = biz();
        return repo.findByBusinessIdOrderByNameAsc(businessId)
                .stream().map(this::toDto).toList();
    }

    public LocationDto create(LocationCreate req) {
        Long businessId = biz();
        String name = (req.name() == null) ? "" : req.name().trim();
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location name is required");
        }
        if (repo.existsByBusinessIdAndNameIgnoreCase(businessId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location already exists");
        }
        Location saved = repo.save(Location.builder()
                .businessId(businessId)
                .name(name)
                .build());
        return toDto(saved);
    }

    public LocationDto update(Long id, LocationUpdate req) {
        Long businessId = biz();
        Location loc = repo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));

        String name = (req.name() == null) ? "" : req.name().trim();
        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location name is required");
        }
        if (repo.existsByBusinessIdAndNameIgnoreCaseAndIdNot(businessId, name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location name already exists");
        }

        loc.setName(name);
        Location saved = repo.save(loc);
        return toDto(saved);
    }

    public void delete(Long id) {
        Long businessId = biz();
        if (!repo.existsByIdAndBusinessId(id, businessId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found");
        }
        repo.deleteById(id);
    }

    private LocationDto toDto(Location l) {
        return new LocationDto(l.getId(), l.getName(), l.getCreatedAt(), l.getUpdatedAt());
    }
}
