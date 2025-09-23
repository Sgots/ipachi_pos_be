package com.ipachi.pos.service;

// ... imports ...
import com.ipachi.pos.dto.ReceiptFileView;
import com.ipachi.pos.dto.StockReceiptDto;
import com.ipachi.pos.model.StockReceipt;
import com.ipachi.pos.repo.StockReceiptRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReceiptService {
    private final StockReceiptRepository repo;
    private final CurrentRequest ctx;

    private Long requireBusiness() {
        Long v = ctx.getBusinessId();
        if (v == null) throw new IllegalStateException("Business ID not found in request headers");
        return v;
    }

    public List<StockReceiptDto> search(String q) {
        Long businessId = requireBusiness();

        var list = (q == null || q.isBlank())
                ? repo.findTop50ByBusinessIdOrderByCreatedAtDesc(businessId)
                : repo.searchByBusinessAndQuery(businessId, q.trim().toLowerCase());

        return list.stream().map(this::toDto).toList();
    }

    public ReceiptFileView getFile(Long id) {
        Long businessId = requireBusiness();

        var e = repo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var ct = (e.getContentType() == null || e.getContentType().isBlank())
                ? "application/octet-stream" : e.getContentType();

        log.debug("Retrieved file for receipt ID: {} and business: {}", id, businessId);
        return new ReceiptFileView(e.getFileName(), ct, e.getData());
    }

    private StockReceiptDto toDto(StockReceipt e) {
        return new StockReceiptDto(
                e.getId(),
                e.getLabel(),
                e.getFileName(),
                e.getContentType(),
                e.getFileSize(),
                "/api/inventory/receipts/" + e.getId() + "/file",
                e.getCreatedAt()
        );
    }
    public StockReceiptDto upload(String label, MultipartFile file) {
        Long businessId = requireBusiness();
        Long userId = requireUser();

        if (label == null || label.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Label is required");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read file");
        }

        var now = OffsetDateTime.now();

        // Build and save entity
        var entity = new StockReceipt();
        entity.setLabel(label.trim());
        entity.setFileName(file.getOriginalFilename());
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setData(data);
        entity.setBusinessId(businessId);
        entity.setUserId(userId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        entity = repo.save(entity);
        log.info("Uploaded receipt {} ({} bytes) for business={}, user={}",
                entity.getId(), entity.getFileSize(), businessId, userId);

        return toDto(entity);
    }

    private Long requireUser() {
        Long userId = ctx.getUserId();
        if (userId == null) throw new IllegalStateException("User ID not found in request headers");
        return userId;
    }
}
