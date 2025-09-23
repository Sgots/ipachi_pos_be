package com.ipachi.pos.controller;

import com.ipachi.pos.dto.*;
import com.ipachi.pos.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Matches the current UI calls in StaffManagement.tsx:
 *  GET    /api/staff
 *  POST   /api/staff                 { firstname, lastname, email(staffNo), roleId, locationId, active, terminalId }
 *  PUT    /api/staff/{id}            { ...same... }
 *  DELETE /api/staff/{id}
 *  POST   /api/staff/{id}/reset-password   -> { password }
 */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService service;

    @GetMapping
    public ResponseEntity<List<StaffDto>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PostMapping
    public ResponseEntity<StaffCreateResponse> create(@RequestBody StaffCreate req) {
        return ResponseEntity.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffDto> update(@PathVariable Long id, @RequestBody StaffUpdate req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<java.util.Map<String, String>> reset(@PathVariable Long id) {
        String pwd = service.resetPassword(id);
        return ResponseEntity.ok(java.util.Map.of("password", pwd));
    }
}
