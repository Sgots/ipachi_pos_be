package com.ipachi.pos.service;

import com.ipachi.pos.dto.*;
import com.ipachi.pos.model.Product;
import com.ipachi.pos.model.StockMovement;
import com.ipachi.pos.model.StockReceipt;
import com.ipachi.pos.repo.*;
import com.ipachi.pos.security.CurrentRequest;  // Added import
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;  // Added import
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;  // Added import
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional  // Added for data consistency
public class StockService {
    private final ProductRepository products;
    private final StockMovementRepository movements;
    private final StockReceiptRepository receipts;
    private final CurrentRequest ctx;  // Added CurrentRequest injection

    // Helper method to validate user context (minimal addition)
    private Long validateUserContext() {
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }
        return userId;
    }

    public List<StockItemDto> list(String q) {
        Long userId = validateUserContext();  // Added user validation

        // pull products for current user (optionally filtered)
        List<Product> base = (q == null || q.isBlank())
                ? products.findByUserIdOrderByNameAsc(userId)  // Updated to include userId
                : products
                .findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrBarcodeContainingIgnoreCaseAndUserId(
                        q.trim(), q.trim(), q.trim(), userId, Pageable.unpaged()  // Updated to include userId
                )
                .getContent(); // because the repo method returns a Page<Product>

        // one query to get all totals for current user
        Map<Long, BigDecimal> totals = movements.totalsByProductIdAndUserId(userId).stream()  // Updated to include userId
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (BigDecimal) r[1]
                ));

        List<StockItemDto> out = new ArrayList<>(base.size());
        for (var p : base) {
            var qty = totals.getOrDefault(p.getId(), BigDecimal.ZERO);
            out.add(new StockItemDto(
                    p.getId(), p.getSku(), p.getBarcode(), p.getName(),
                    p.getUnit() == null ? null : p.getUnit().getId(),
                    p.getUnit() == null ? null : p.getUnit().getName(),
                    p.getUnit() == null ? null : p.getUnit().getAbbr(),
                    qty,
                    null // or p.getLowStock() if you have it on Product
            ));
        }
        return out;
    }

    public RestockResponse restock(Long productId, RestockRequest req) {
        Long userId = validateUserContext();  // Added user validation

        var p = products.findByIdAndUserId(productId, userId)  // Updated to include userId
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var qty = req.quantity();
        if (qty == null || qty.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive");
        }

        StockReceipt receipt = null;
        if (req.receiptId() != null) {
            receipt = receipts.findByIdAndUserId(req.receiptId(), userId)  // Updated to include userId
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receipt not found"));
        }

        var mv = StockMovement.builder()
                .product(p)
                .quantityDelta(qty)
                .receipt(receipt)
                .note(req.note())
                .userId(userId)  // Added userId
                .createdAt(OffsetDateTime.now())  // Added timestamps
                .updatedAt(OffsetDateTime.now())
                .build();
        movements.save(mv);

        var current = movements.sumByProductIdAndUserId(p.getId(), userId);  // Updated to include userId
        return new RestockResponse(p.getId(), current == null ? BigDecimal.ZERO : current);
    }
}