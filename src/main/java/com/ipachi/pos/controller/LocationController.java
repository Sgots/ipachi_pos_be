package com.ipachi.pos.controller;

import com.ipachi.pos.dto.LocationCreate;
import com.ipachi.pos.dto.LocationDto;
import com.ipachi.pos.dto.LocationUpdate;
import com.ipachi.pos.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Minimal controller that matches the UI calls in StaffManagement.tsx:
 *  GET    /api/locations
 *  POST   /api/locations              { name }
 *  PUT    /api/locations/{id}         { name }
 *  DELETE /api/locations/{id}
 *
 * The frontend already tolerates both raw arrays and {data: ...}, so we return raw.
 */
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService service;

    @GetMapping
    public ResponseEntity<List<LocationDto>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    public ResponseEntity<LocationDto> create(@RequestBody LocationCreate req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationDto> update(@PathVariable Long id, @RequestBody LocationUpdate req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
