package com.ipachi.pos.service;

// src/main/java/com/ipachi/pos/service/StockService.java

import com.ipachi.pos.dto.*;
import com.ipachi.pos.model.Product;
import com.ipachi.pos.model.StockMovement;
import com.ipachi.pos.model.StockReceipt;
import com.ipachi.pos.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {
    private final ProductRepository products;
    private final StockMovementRepository movements;
    private final StockReceiptRepository receipts;


    public List<StockItemDto> list(String q) {
        // pull products (optionally filtered)
        List<Product> base = (q == null || q.isBlank())
                ? products.findAll()
                : products
                .findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
                        q.trim(), q.trim(), q.trim(), Pageable.unpaged()
                )
                .getContent(); // because the repo method returns a Page<Product>

        // one query to get all totals
        Map<Long, BigDecimal> totals = movements.totalsByProduct().stream()
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
        var p = products.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var qty = req.quantity();
        if (qty == null || qty.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive");
        }

        StockReceipt receipt = null;
        if (req.receiptId() != null) {
            receipt = receipts.findById(req.receiptId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receipt not found"));
        }

        var mv = StockMovement.builder()
                .product(p)
                .quantityDelta(qty)
                .receipt(receipt)
                .note(req.note())
                .build();
        movements.save(mv);

        var current = movements.sumByProductId(p.getId());
        return new RestockResponse(p.getId(), current == null ? BigDecimal.ZERO : current);
    }
}
