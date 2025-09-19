package com.ipachi.pos.service;

import com.ipachi.pos.dto.ReceiptFileView;
import com.ipachi.pos.dto.StockReceiptDto;
import com.ipachi.pos.model.StockReceipt;
import com.ipachi.pos.repo.StockReceiptRepository;
import com.ipachi.pos.security.CurrentRequest;  // Added import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;  // Added import
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;  // Added import
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;  // Added import
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j  // Added for minimal logging
@Transactional  // Added for data consistency
public class StockReceiptService {
    private final StockReceiptRepository repo;
    private final CurrentRequest ctx;  // Added CurrentRequest injection

    // Helper method to validate user context (minimal addition)
    private Long validateUserContext() {
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }
        return userId;
    }

    public StockReceiptDto upload(String label, MultipartFile file) {
        Long userId = validateUserContext();  // Added user validation

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File required");
        }
        try {
            var e = StockReceipt.builder()
                    .label((label == null || label.isBlank()) ? file.getOriginalFilename() : label.trim())
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .data(file.getBytes())
                    .userId(userId)  // Added userId
                    .createdAt(OffsetDateTime.now())  // Added timestamp
                    .updatedAt(OffsetDateTime.now())
                    .build();
            e = repo.save(e);
            log.debug("Uploaded receipt '{}' for user: {}", e.getLabel(), userId);  // Added minimal logging
            return toDto(e);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read file");
        }
    }

    public List<StockReceiptDto> search(String q) {
        Long userId = validateUserContext();  // Added user validation

        var list = (q == null || q.isBlank())
                ? repo.findTop50ByUserIdOrderByCreatedAtDesc(userId)  // Updated to include userId
                : repo.searchAndUserId(q.trim().toLowerCase(), userId);  // Updated to include userId
        return list.stream().map(this::toDto).toList();
    }

    public ReceiptFileView getFile(Long id) {
        Long userId = validateUserContext();  // Added user validation

        var e = repo.findByIdAndUserId(id, userId)  // Updated to include userId
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var ct = (e.getContentType() == null || e.getContentType().isBlank())
                ? "application/octet-stream" : e.getContentType();
        log.debug("Retrieved file for receipt ID: {} and user: {}", id, userId);  // Added minimal logging
        return new ReceiptFileView(e.getFileName(), ct, e.getData());
    }

    private StockReceiptDto toDto(StockReceipt e) {
        return new StockReceiptDto(
                e.getId(),
                e.getLabel(),
                e.getFileName(),
                e.getContentType(),
                e.getFileSize(),
                // URL the frontend can open in a new tab:
                "/api/inventory/receipts/" + e.getId() + "/file",
                e.getCreatedAt()
        );
    }
}