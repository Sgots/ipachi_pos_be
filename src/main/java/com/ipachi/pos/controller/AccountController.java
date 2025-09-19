package com.ipachi.pos.controller;

import com.ipachi.pos.model.BusinessProfile;
import com.ipachi.pos.model.FileAsset;
import com.ipachi.pos.model.Settings;
import com.ipachi.pos.model.Subscription;
import com.ipachi.pos.model.User;
import com.ipachi.pos.model.UserProfile;
import com.ipachi.pos.security.CurrentRequest;
import com.ipachi.pos.service.BusinessService;
import com.ipachi.pos.service.SettingsService;
import com.ipachi.pos.service.SubscriptionService;
import com.ipachi.pos.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AccountController {

    @Autowired private UserService userService;
    @Autowired private BusinessService businessService;
    @Autowired private SettingsService settingsService;
    @Autowired private SubscriptionService subscriptionService;
    @Autowired private CurrentRequest currentRequest;

    // ----------------------------
    // About Me (UserProfile)
    // ----------------------------
    @GetMapping("/user-profile")
    public ResponseEntity<?> getUserProfile() {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        UserProfile profile = userService.getUserProfile(userId);
        if (profile != null) {
            return ResponseEntity.ok(mapToUserProfileDto(profile));
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/user-profile")
    public ResponseEntity<String> updateUserProfile(@RequestBody UserProfile profile) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        userService.updateUserProfile(userId, profile);
        return ResponseEntity.ok("updated");
    }

    // ----------------------------
    // My Business (BusinessProfile)
    // ----------------------------
    @GetMapping("/business-profile")
    public ResponseEntity<?> getBusinessProfile() {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        BusinessProfile profile = businessService.getBusinessProfile(userId);
        if (profile != null) {
            return ResponseEntity.ok(mapToBusinessProfileDto(profile));
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/business-profile")
    public ResponseEntity<String> updateBusinessProfile(@RequestBody BusinessProfile profile) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        businessService.updateBusinessProfile(userId, profile);
        return ResponseEntity.ok("updated");
    }

    // ----------------------------
    // Subscriptions
    // ----------------------------
    @GetMapping("/subscription")
    public ResponseEntity<?> getSubscription() {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Subscription sub = subscriptionService.getSubscription(userId);
        return ResponseEntity.ok(sub != null ? sub : new Subscription());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<String> subscribe(@RequestBody Map<String, String> body) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        String plan = body.get("plan");
        if (plan == null || plan.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Plan is required");
        }

        subscriptionService.subscribe(userId, plan);
        return ResponseEntity.ok("subscribed to " + plan);
    }

    // ----------------------------
    // Settings
    // ----------------------------
    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }

        Settings settings = settingsService.getSettings(userId);
        return ResponseEntity.ok(settings != null ? settings : new Settings());
    }

    @PutMapping("/settings")
    public ResponseEntity<String> updateSettings(@RequestBody Settings settings) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        settingsService.updateSettings(userId, settings);
        return ResponseEntity.ok("updated");
    }

    // ----------------------------
    // Security - Change Password
    // ----------------------------
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody Map<String, String> body) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Passwords are required");
        }

        if (userService.changePassword(userId, oldPassword, newPassword)) {
            return ResponseEntity.ok("updated");
        }
        return ResponseEntity.badRequest().body("Invalid old password");
    }

    // ----------------------------
    // Account - Deactivate
    // ----------------------------
    @DeleteMapping("/deactivate")
    public ResponseEntity<String> deactivate() {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        userService.deactivate(userId);
        return ResponseEntity.ok("deactivated");
    }

    // ----------------------------
    // Assets (BLOB) — Authenticated GETs with ownership checks
    // ----------------------------

    /**
     * Serve the user's profile picture by assetId.
     * Requires authentication; verifies the asset belongs to the current user.
     */
    @GetMapping("/user-profile/picture/file/{assetId}")
    public ResponseEntity<ByteArrayResource> getUserProfilePicture(@PathVariable String assetId) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            UserProfile profile = userService.getUserProfile(userId);
            if (profile == null || profile.getPictureAsset() == null ||
                    profile.getPictureAsset().getId() == null ||
                    !profile.getPictureAsset().getId().equals(assetId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            FileAsset asset = profile.getPictureAsset();
            if (asset.getData() == null || asset.getData().length == 0) {
                return ResponseEntity.notFound().build();
            }

            return buildBlobResponse(asset);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Serve the user's ID document by assetId.
     * Requires authentication; verifies the asset belongs to the current user.
     */
    @GetMapping("/user-profile/id-doc/file/{assetId}")
    public ResponseEntity<ByteArrayResource> getUserIdDocument(@PathVariable String assetId) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            UserProfile profile = userService.getUserProfile(userId);
            if (profile == null || profile.getIdDocAsset() == null ||
                    profile.getIdDocAsset().getId() == null ||
                    !profile.getIdDocAsset().getId().equals(assetId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            FileAsset asset = profile.getIdDocAsset();
            if (asset.getData() == null || asset.getData().length == 0) {
                return ResponseEntity.notFound().build();
            }

            return buildBlobResponse(asset);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Serve the business logo by assetId.
     * Requires authentication; verifies the asset belongs to the current user's business.
     */
    @GetMapping("/business-profile/logo/file/{assetId}")
    public ResponseEntity<ByteArrayResource> getBusinessLogo(@PathVariable String assetId) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            BusinessProfile bp = businessService.getBusinessProfile(userId);
            if (bp == null || bp.getLogoAsset() == null ||
                    bp.getLogoAsset().getId() == null ||
                    !bp.getLogoAsset().getId().equals(assetId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            FileAsset asset = bp.getLogoAsset();
            if (asset.getData() == null || asset.getData().length == 0) {
                return ResponseEntity.notFound().build();
            }

            return buildBlobResponse(asset);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ----------------------------
    // Helpers
    // ----------------------------

    private ResponseEntity<ByteArrayResource> buildBlobResponse(FileAsset asset) {
        String ct = asset.getContentType() != null ? asset.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String filename = asset.getFilename() != null ? asset.getFilename() : "file";
        ByteArrayResource resource = new ByteArrayResource(asset.getData());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(asset.getSize() != 0 ? asset.getSize() : asset.getData().length))
                .body(resource);
    }

    /**
     * Get current user id from either:
     *  - CurrentRequest (if interceptor populated it), or
     *  - Spring Security context (JWT), using the username/email to find the User record.
     */
    private Long currentUserId() {
        Long userId = currentRequest.getUserId(); // may be null if interceptor excluded this path
        if (userId != null) return userId;

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            Object principal = auth.getPrincipal();
            String username;
            if (principal instanceof UserDetails ud) {
                username = ud.getUsername();
            } else {
                username = String.valueOf(principal);
            }
            User user = userService.findByEmail(username);
            return user != null ? user.getId() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    // DTO mapping methods to avoid lazy loading issues
    private Map<String, Object> mapToUserProfileDto(UserProfile profile) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", profile.getId());
        dto.put("title", profile.getTitle());
        dto.put("gender", profile.getGender());
        dto.put("dob", profile.getDob());
        dto.put("idType", profile.getIdType() != null ? profile.getIdType().toString() : null);
        dto.put("idNumber", profile.getIdNumber());
        dto.put("postalAddress", profile.getPostalAddress());
        dto.put("physicalAddress", profile.getPhysicalAddress());
        dto.put("city", profile.getCity());
        dto.put("country", profile.getCountry());
        dto.put("areaCode", profile.getAreaCode());
        dto.put("phone", profile.getPhone());
        dto.put("hasPicture", profile.getPictureAsset() != null);
        dto.put("hasIdDoc", profile.getIdDocAsset() != null);

        if (profile.getPictureAsset() != null) {
            dto.put("pictureUrl", "/api/user-profile/picture/file/" + profile.getPictureAsset().getId());
            dto.put("pictureAssetId", profile.getPictureAsset().getId());
        } else {
            dto.put("pictureUrl", null);
        }
        if (profile.getIdDocAsset() != null) {
            dto.put("idDocUrl", "/api/user-profile/id-doc/file/" + profile.getIdDocAsset().getId());
            dto.put("idDocAssetId", profile.getIdDocAsset().getId());
        } else {
            dto.put("idDocUrl", null);
        }

        return dto;
    }

    private Map<String, Object> mapToBusinessProfileDto(BusinessProfile profile) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", profile.getId());
        dto.put("name", profile.getName());
        dto.put("location", profile.getLocation());
        dto.put("hasLogo", profile.getLogoAsset() != null);
        if (profile.getLogoAsset() != null) {
            dto.put("logoUrl", "/api/business-profile/logo/file/" + profile.getLogoAsset().getId());
            dto.put("logoAssetId", profile.getLogoAsset().getId());
        } else {
            dto.put("logoUrl", null);
        }
        return dto;
    }
}
