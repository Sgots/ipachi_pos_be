// src/main/java/com/ipachi/pos/service/StockService.java
package com.ipachi.pos.service;

import com.ipachi.pos.dto.RestockRequest;
import com.ipachi.pos.dto.RestockResponse;
import com.ipachi.pos.dto.StockItemDto;
import com.ipachi.pos.model.Product;
import com.ipachi.pos.model.StockMovement;
import com.ipachi.pos.model.StockReceipt;
import com.ipachi.pos.repo.ProductRepository;
import com.ipachi.pos.repo.StockMovementRepository;
import com.ipachi.pos.repo.StockReceiptRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StockService {
    private final ProductRepository products;
    private final StockMovementRepository movements;
    private final StockReceiptRepository receipts;
    private final CurrentRequest ctx;

    /* ===== Helpers: business scoping + audit who/terminal ===== */
    private Long biz() {
        Long v = ctx.getBusinessId();
        if (v == null) throw new IllegalStateException("X-Business-Id missing");
        return v;
    }
    private Long user() {
        Long v = ctx.getUserId();
        if (v == null) throw new IllegalStateException("X-User-Id missing");
        return v;
    }
    private Long term() { return ctx.getTerminalId(); }

    /** List stock items for the current business (optional search q). */
    public List<StockItemDto> list(String q) {
        Long businessId = biz();

        // Products for this business (optionally filtered by q)
        List<Product> base = (q == null || q.isBlank())
                ? products.findByBusinessIdOrderByNameAsc(businessId)
                : products.searchByBusiness(businessId, q.trim(), Pageable.unpaged()).getContent();

        // One query to get totals for all products in this business
        Map<Long, BigDecimal> totals = movements.totalsByProductIdAndBusinessId(businessId).stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> (BigDecimal) r[1]
                ));

        List<StockItemDto> out = new ArrayList<>(base.size());
        for (var p : base) {
            var qty = totals.getOrDefault(p.getId(), BigDecimal.ZERO);
            out.add(new StockItemDto(
                    p.getId(),
                    p.getSku(),
                    p.getBarcode(),
                    p.getName(),
                    p.getUnit() == null ? null : p.getUnit().getId(),
                    p.getUnit() == null ? null : p.getUnit().getName(),
                    p.getUnit() == null ? null : p.getUnit().getAbbr(),
                    qty,
                    p.getLowStock()
            ));
        }
        return out;
    }

    /** Restock a product for the current business, stamping who + terminal. */
    public RestockResponse restock(Long productId, RestockRequest req) {
        Long businessId = biz();
        Long userId = user();

        var p = products.findByIdAndBusinessId(productId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        var qty = req.quantity();
        if (qty == null || qty.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive");
        }

        StockReceipt receipt = null;
        if (req.receiptId() != null) {
            receipt = receipts.findByIdAndBusinessId(req.receiptId(), businessId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receipt not found"));
        }

        var mv = StockMovement.builder()
                .businessId(businessId)     // owner scope
                .createdByUserId(userId)    // who did it
                .userId(userId)             // <-- satisfy NOT NULL 'user_id' from BaseOwnedEntity
                .terminalId(term())         // optional terminal audit
                .product(p)
                .quantityDelta(qty)
                .receipt(receipt)
                .note(req.note())
                .createdAt(OffsetDateTime.now())
                .build();
        movements.save(mv);

        var current = movements.sumByProductIdAndBusinessId(p.getId(), businessId);
        return new RestockResponse(p.getId(), current == null ? BigDecimal.ZERO : current);
    }
}
