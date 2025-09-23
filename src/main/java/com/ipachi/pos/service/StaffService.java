// src/main/java/com/ipachi/pos/service/StaffService.java
package com.ipachi.pos.service;

import com.ipachi.pos.dto.*;
import com.ipachi.pos.model.*;
import com.ipachi.pos.repo.*;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StaffService {

    private final StaffMemberRepository staffRepo;
    private final RoleRepository roleRepo;
    private final LocationRepository locationRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final CurrentRequest ctx;

    /* ---------- Context helpers ---------- */
    private Long biz() {
        Long v = ctx.getBusinessId();
        if (v == null) throw new IllegalStateException("X-Business-Id missing");
        return v;
    }

    /* ---------- API ---------- */

    @Transactional(readOnly = true)
    public List<StaffDto> list() {
        Long businessId = biz();
        return staffRepo.findByBusinessIdOrderByIdAsc(businessId)
                .stream().map(this::toDto).toList();
    }

    public StaffCreateResponse create(StaffCreate req) {
        Long businessId = biz();

        String staffNumber = safe(req.email()); // UI sends Staff Number in "email" field
        if (staffNumber.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff Number is required");
        if (userRepo.existsByUsernameIgnoreCase(staffNumber))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Staff Number already exists");

        // role & location must belong to this business
        Role role = roleRepo.findByIdAndBusinessId(req.roleId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));
        Location location = locationRepo.findByIdAndBusinessId(req.locationId(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location not found"));

        // Generate password and create user
        String rawPwd = generatePassword();
        User user = User.builder()
                .username(staffNumber)                         // staff number
                .email(null)                                   // optional, not used for staff
                .passwordHash(encoder.encode(rawPwd))
                .role(UserRole.STAFF)                          // keep using your enum
                .enabled(req.active() != null ? req.active() : true)
                .businessProfileId(businessId)                 // <<< IMPORTANT: link to current business
                .terminalId(req.terminalId()) // optional
                .build();
        user = userRepo.save(user);

        StaffMember staff = StaffMember.builder()
                .businessId(businessId)
                .user(user)
                .firstName(safe(req.firstname()))
                .lastName(safe(req.lastname()))
                .role(role)
                .location(location)
                .active(req.active() != null ? req.active() : true)
                .build();
        staff = staffRepo.save(staff);

        log.info("Created staff id={} userId={} businessId={}", staff.getId(), user.getId(), businessId);

        return new StaffCreateResponse(toDto(staff), rawPwd);
    }

    public StaffDto update(Long id, StaffUpdate req) {
        Long businessId = biz();

        StaffMember staff = staffRepo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));

        // If staff number (username) changes, enforce uniqueness
        String newStaffNo = safe(req.email());
        if (!newStaffNo.isBlank() && !newStaffNo.equalsIgnoreCase(staff.getUser().getUsername())) {
            if (userRepo.existsByUsernameIgnoreCase(newStaffNo))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Staff Number already exists");
            staff.getUser().setUsername(newStaffNo);
        }

        // Update role & location (scoped)
        if (req.roleId() != null) {
            Role role = roleRepo.findByIdAndBusinessId(req.roleId(), businessId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));
            staff.setRole(role);
        }
        if (req.locationId() != null) {
            Location location = locationRepo.findByIdAndBusinessId(req.locationId(), businessId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location not found"));
            staff.setLocation(location);
        }

        staff.setFirstName(safe(req.firstname()));
        staff.setLastName(safe(req.lastname()));
        if (req.active() != null) {
            staff.setActive(req.active());
            staff.getUser().setEnabled(req.active());
        }

        // Always ensure linkage stays correct
        staff.getUser().setBusinessProfileId(businessId);
        staff.getUser().setTerminalId(req.terminalId());

        // persist both sides
        userRepo.save(staff.getUser());
        staffRepo.save(staff);

        return toDto(staff);
    }

    public void delete(Long id) {
        Long businessId = biz();
        StaffMember staff = staffRepo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));

        // Disable login but keep history
        User u = staff.getUser();
        u.setEnabled(false);
        userRepo.save(u);

        staffRepo.delete(staff);
    }

    public String resetPassword(Long id) {
        Long businessId = biz();
        StaffMember staff = staffRepo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));

        String raw = generatePassword();
        staff.getUser().setPasswordHash(encoder.encode(raw));
        // guard linkage (in case older rows missed it)
        staff.getUser().setBusinessProfileId(businessId);
        userRepo.save(staff.getUser());
        return raw;
    }

    /* ---------- mapping & helpers ---------- */

    private StaffDto toDto(StaffMember s) {
        return new StaffDto(
                s.getId(),
                s.getFirstName(),
                s.getLastName(),
                // UI column labeled "Staff Number" expects username here
                s.getUser() == null ? null : s.getUser().getUsername(),
                s.getRole() == null ? null : s.getRole().getId(),
                s.getRole() == null ? null : s.getRole().getName(),
                s.getLocation() == null ? null : s.getLocation().getId(),
                s.getLocation() == null ? null : s.getLocation().getName(),
                s.isActive(),
                s.getUser() == null ? null :
                        (s.getUser().getTerminalId() == null ? null : String.valueOf(s.getUser().getTerminalId()))
        );
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static Long parseTerminalId(String v) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        try { return Long.valueOf(t); } catch (NumberFormatException ignore) { return null; }
    }

    private static final SecureRandom RNG = new SecureRandom();
    private static String generatePassword() {
        // 12 hex chars (~48 bits entropy) + 2 symbols
        byte[] bytes = new byte[6];
        RNG.nextBytes(bytes);
        String hex = HexFormat.of().formatHex(bytes);
        String symbols = "!@#$%^&*";
        char sym1 = symbols.charAt(RNG.nextInt(symbols.length()));
        char sym2 = symbols.charAt(RNG.nextInt(symbols.length()));
        return hex.substring(0, 6) + sym1 + hex.substring(6) + sym2;
    }
}
