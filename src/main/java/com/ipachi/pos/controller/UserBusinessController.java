// src/main/java/com/ipachi/pos/controller/UserBusinessController.java
package com.ipachi.pos.controller;

import com.ipachi.pos.model.BusinessProfile;
import com.ipachi.pos.model.User;
import com.ipachi.pos.repo.BusinessProfileRepository;
import com.ipachi.pos.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal controller to support:
 *   GET /api/users/{userId}/business-profile
 *
 * Returns a payload that works with existing FE parsing:
 * it can be accessed via response.data OR response.data.data.
 * Fields include both "id" and "businessId" for compatibility.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserBusinessController {

    private final UserRepository users;
    private final BusinessProfileRepository businesses;

    @GetMapping("/{userId}/business-profile")
    public ResponseEntity<?> getBusinessProfileByUser(@PathVariable Long userId) {
        // 1) Find user
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 2) Read business id from user and validate
        Long bpId = user.getBusinessProfileId();
        if (bpId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has no business profile set");
        }

        // 3) Find business profile by id
        BusinessProfile bp = businesses.findById(bpId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business profile not found"));

        // 4) Build response payload
        String logoUrl = (bp.getLogoAsset() != null) ? ("/api/business-profile/logo/file/" + bp.getLogoAsset().getId()) : null;

        Map<String, Object> biz = new HashMap<>();
        biz.put("id", bp.getId());
        biz.put("businessId", bp.getId());     // FE fallback: d.businessId || d.id
        biz.put("name", bp.getName());
        biz.put("location", bp.getLocation());
        biz.put("logoUrl", logoUrl);
        biz.put("userId", user.getId());

        Map<String, Object> envelope = new HashMap<>();
        envelope.put("code", "OK");
        envelope.put("message", "Business profile");
        envelope.put("data", biz);

        return ResponseEntity.ok(envelope);
    }

}
