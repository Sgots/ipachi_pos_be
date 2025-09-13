// src/main/java/com/ipachi/pos/service/StockReceiptService.java
package com.ipachi.pos.service;

import com.ipachi.pos.dto.ReceiptFileView;
import com.ipachi.pos.dto.StockReceiptDto;
import com.ipachi.pos.model.StockReceipt;
import com.ipachi.pos.repo.StockReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockReceiptService {
    private final StockReceiptRepository repo;

    public StockReceiptDto upload(String label, MultipartFile file) {
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
                    .build();
            e = repo.save(e);
            return toDto(e);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read file");
        }
    }

    public List<StockReceiptDto> search(String q) {
        var list = (q == null || q.isBlank())
                ? repo.findTop50ByOrderByCreatedAtDesc()
                : repo.search(q.trim().toLowerCase());
        return list.stream().map(this::toDto).toList();
    }

    public ReceiptFileView getFile(Long id) {
        var e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var ct = (e.getContentType() == null || e.getContentType().isBlank())
                ? "application/octet-stream" : e.getContentType();
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
