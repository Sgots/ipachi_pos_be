// src/main/java/com/ipachi/pos/controller/InventoryHistoryController.java
package com.ipachi.pos.controller;

import com.ipachi.pos.dto.ReceiptItemView;
import com.ipachi.pos.dto.RestockHistoryView;
import com.ipachi.pos.model.StockReceipt;
import com.ipachi.pos.repo.RestockHistoryRepository;
import com.ipachi.pos.repo.StockReceiptRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryHistoryController {

    private final RestockHistoryRepository historyRepo;
    private final StockReceiptRepository receiptRepo;
    private final CurrentRequest currentRequest;

    public InventoryHistoryController(RestockHistoryRepository historyRepo, StockReceiptRepository receiptRepo, CurrentRequest currentRequest) {
        this.historyRepo = historyRepo;
        this.receiptRepo = receiptRepo;
        this.currentRequest = currentRequest;
    }

    private Long biz() {
        Long id = currentRequest.getBusinessId();
        if (id == null) throw new IllegalStateException("X-Business-Id missing");
        return id;
    }
    // TODO: replace with your actual context helpers

    @GetMapping("/restock-history")
    public ResponseEntity<List<RestockHistoryView>> history(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "from", required = false) LocalDate from,
            @RequestParam(value = "to", required = false) LocalDate to
    ) {
        OffsetDateTime fromTs = (from == null) ? null : from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTs   = (to   == null) ? null : to.plusDays(1).atStartOfDay().minusSeconds(1).atOffset(ZoneOffset.UTC);

        var list = historyRepo.restockHistory(biz(), fromTs, toTs, q);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/receipts/{id}/items")
    public ResponseEntity<List<ReceiptItemView>> items(@PathVariable Long id) {
        // simple existence + scope check
        StockReceipt r = receiptRepo.findByIdAndBusinessId(id, biz())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));
        return ResponseEntity.ok(historyRepo.receiptItems(biz(), r.getId()));
    }
}
