package com.ipachi.pos.service;

import com.ipachi.pos.model.FileAsset;
import com.ipachi.pos.repo.FileAssetRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor  // This generates constructor for final fields
@Slf4j  // This adds logging without affecting constructor
public class FileStorageDbService {

    // These final fields will be injected via constructor
    private final FileAssetRepository repo;
    private final CurrentRequest ctx;  // Add this for userId from headers

    /**
     * Save a file asset associated with the current user from request context
     */
    public FileAsset save(MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) {
            log.debug("Empty file provided, returning null");
            return null;
        }

        // Get userId from request context
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }

        log.debug("Saving file '{}' for user ID: {}", file.getOriginalFilename(), userId);

        // Use builder pattern with userId
        var asset = FileAsset.builder()
                .filename(safeName(file.getOriginalFilename()))
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .size(file.getSize())
                .data(file.getBytes())
                .userId(userId)  // Set userId from context
                .build();

        try {
            FileAsset savedAsset = repo.save(asset);
            log.info("Successfully saved FileAsset '{}' with ID: {} for user ID: {}",
                    savedAsset.getFilename(), savedAsset.getId(), userId);
            return savedAsset;
        } catch (Exception e) {
            log.error("Failed to save FileAsset for user ID: {} - Error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Overloaded method that accepts explicit userId (for backward compatibility)
     */
    public FileAsset save(MultipartFile file, Long userId) throws java.io.IOException {
        if (file == null || file.isEmpty()) {
            log.debug("Empty file provided, returning null");
            return null;
        }

        log.debug("Saving file '{}' for user ID: {}", file.getOriginalFilename(), userId);

        var asset = FileAsset.builder()
                .filename(safeName(file.getOriginalFilename()))
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .size(file.getSize())
                .data(file.getBytes())
                .userId(userId)
                .build();

        try {
            FileAsset savedAsset = repo.save(asset);
            log.info("Successfully saved FileAsset '{}' with ID: {} for user ID: {}",
                    savedAsset.getFilename(), savedAsset.getId(), userId);
            return savedAsset;
        } catch (Exception e) {
            log.error("Failed to save FileAsset for user ID: {} - Error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get a file asset by its ID (with user authorization check)
     */
    public FileAsset get(String id) {
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }

        return repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + id));
    }

    /**
     * Create a safe filename by removing dangerous characters
     */
    private static String safeName(String n) {
        return n == null ? "file" : n.replaceAll("[\\r\\n]", "_");
    }
}