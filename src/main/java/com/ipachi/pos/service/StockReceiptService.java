package com.ipachi.pos.service;

import com.ipachi.pos.dto.ReceiptFileView;
import com.ipachi.pos.dto.StockReceiptDto;
import com.ipachi.pos.model.StockReceipt;
import com.ipachi.pos.repo.StockReceiptRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    private Long requireUser() {
        Long userId = ctx.getUserId();
        if (userId == null) throw new IllegalStateException("User ID not found in request headers");
        return userId;
    }

    public List<StockReceiptDto> search(String q) {
        Long businessId = requireBusiness();

        var list = (q == null || q.isBlank())
                ? repo.findTop50ByBusinessIdOrderByReceiptAtDescCreatedAtDesc(businessId)
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
                e.getCreatedAt(),
                e.getReceiptAt()
        );
    }

    /**
     * Upload a receipt and persist its effective "receipt date".
     * @param label      user label/reference
     * @param file       file contents
     * @param receiptDay optional calendar day (LocalDate). If null, defaults to "today (UTC)".
     */
    public StockReceiptDto upload(String label, MultipartFile file, LocalDate receiptDay) {
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

        // When not supplied, use "now" in UTC; otherwise use start-of-day UTC for the chosen date.
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime receiptAt = receiptDay == null
                ? now
                : receiptDay.atStartOfDay().atOffset(ZoneOffset.UTC);

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
        entity.setReceiptAt(receiptAt);

        entity = repo.save(entity);
        log.info("Uploaded receipt {} ({} bytes) for business={}, user={}, receiptAt={}",
                entity.getId(), entity.getFileSize(), businessId, userId, entity.getReceiptAt());

        return toDto(entity);
    }
}
