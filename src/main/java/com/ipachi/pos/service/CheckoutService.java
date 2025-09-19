package com.ipachi.pos.service;

import com.ipachi.pos.dto.CheckoutRequest;
import com.ipachi.pos.dto.TillItem;
import com.ipachi.pos.model.Product;
import com.ipachi.pos.model.StockMovement;
import com.ipachi.pos.model.Transaction;
import com.ipachi.pos.model.TransactionLine;
import com.ipachi.pos.repo.ProductRepository;
import com.ipachi.pos.repo.StockMovementRepository;
import com.ipachi.pos.repo.TransactionLineRepository;
import com.ipachi.pos.repo.TransactionRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final TransactionRepository txRepo;
    private final TransactionLineRepository lineRepo;
    private final ProductRepository productRepo;        // NEW: Product repository
    private final StockMovementRepository stockRepo;    // NEW: Stock movement repository
    private final CurrentRequest ctx;

    /**
     * Persist a transaction and its line items, update inventory levels,
     * and return the saved Transaction (with generated ID and total).
     */
    @Transactional
    public Transaction checkout(CheckoutRequest req) {
        // Validate user context from headers
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request headers");
        }

        log.info("Processing checkout for userId: {}, items: {}", userId, req.getItems().size());

        // 1) Calculate total FIRST before creating transaction
        BigDecimal total = calculateTotal(req.getItems());

        // 2) Create transaction head with total already calculated
        Transaction tx = Transaction.builder()
                .customerName(req.getCustomerName() != null && !req.getCustomerName().isBlank()
                        ? req.getCustomerName()
                        : "Walk-in")
                .userId(userId)
                .total(total)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Single save - total is already set
        tx = txRepo.save(tx);
        log.debug("Created transaction ID: {} with total: {} for user: {}", tx.getId(), total, userId);

        // 3) Process line items and inventory updates
        List<TillItem> items = req.getItems();
        List<TransactionLine> savedLines = new ArrayList<>();

        if (items != null && !items.isEmpty()) {
            for (TillItem item : items) {
                savedLines.add(createAndSaveLineItem(tx, item, userId));
            }
        }

        // 4) Update transaction timestamp
        tx.setUpdatedAt(OffsetDateTime.now());
        txRepo.save(tx);

        log.info("Completed checkout - Transaction ID: {}, Total: {}, Lines: {}, User: {}",
                tx.getId(), total, savedLines.size(), userId);

        return tx;
    }

    /**
     * Create and save a transaction line item, and update inventory via stock movement
     */
    private TransactionLine createAndSaveLineItem(Transaction tx, TillItem item, Long userId) {
        BigDecimal unit = safe(BigDecimal.valueOf(item.getPrice()));
        int qty = item.getQty() == 0 ? 0 : item.getQty();
        BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(qty));

        // Create line item
        TransactionLine line = TransactionLine.builder()
                .transaction(tx)
                .sku(item.getSku())
                .name(item.getName() != null ? item.getName() : item.getSku())
                .unitPrice(unit)
                .qty(qty)
                .lineTotal(lineTotal)
                .userId(userId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Save line item
        TransactionLine savedLine = lineRepo.save(line);
        log.debug("Saved transaction line ID: {} for transaction: {}", savedLine.getId(), tx.getId());

        // Update inventory via stock movement (negative delta for sales)
        if (qty > 0) {
            updateStockForSale(item.getSku(), qty, "Sale - TX-" + tx.getId(), userId);
        }

        return savedLine;
    }

    /**
     * Update stock for a sale by creating a negative stock movement
     */
    /**
     * Update stock for a sale by creating a negative stock movement
     */
    @Transactional
    public void updateStockForSale(String sku, int quantityToDeduct, String reference, Long userId) {
        if (sku == null || sku.trim().isEmpty() || quantityToDeduct <= 0) {
            log.debug("Skipping stock update - invalid SKU or zero quantity: sku={}, qty={}", sku, quantityToDeduct);
            return;
        }

        try {
            // 1) Find the product by SKU
            Product product = productRepo.findBySkuIgnoreCaseAndUserId(sku, userId)
                    .orElseGet(() -> {
                        log.warn("Product not found for SKU: {}", sku);
                        return null;
                    });

            if (product == null) {
                log.warn("Cannot update stock - product not found for SKU: {}", sku);
                return;
            }

            // Verify user ownership
            if (!product.getUserId().equals(userId)) {
                log.warn("User {} cannot access product {} owned by user {}", userId, sku, product.getUserId());
                return;
            }

            // 2) Get current stock level
            BigDecimal currentStock = getCurrentStockLevel(product.getId(), userId);
            log.debug("Current stock for SKU {} (product ID {}): {}", sku, product.getId(), currentStock);

            // 3) Check for sufficient stock
            if (currentStock.compareTo(BigDecimal.valueOf(quantityToDeduct)) < 0) {
                log.warn("Insufficient stock for SKU {}: requested={}, available={}",
                        sku, quantityToDeduct, currentStock);
                throw new RuntimeException(String.format(
                        "Insufficient stock for %s: requested %d, available %.4f",
                        product.getName(), quantityToDeduct, currentStock.doubleValue()));
            }

            // 4) Create negative stock movement for sale - FIXED FORMAT STRING
            StockMovement saleMovement = StockMovement.builder()
                    .product(product)
                    .quantityDelta(BigDecimal.valueOf(quantityToDeduct).negate()) // Negative for sales
                    .note(String.format("Sale - %s (TX-%s)", reference, reference)) // FIXED: %s for both
                    .userId(userId)
                    .createdAt(OffsetDateTime.now())
                    .build();

            StockMovement savedMovement = stockRepo.saveAndFlush(saleMovement);

            // 5) Verify the update
            BigDecimal newStock = getCurrentStockLevel(product.getId(), userId);

            log.info("Stock updated for SKU {}: {} -> {} (deducted {}, movement ID: {})",
                    sku, currentStock, newStock, quantityToDeduct, savedMovement.getId());

        } catch (Exception e) {
            log.error("Failed to update stock for sale - SKU {}: {}", sku, e.getMessage(), e);
            throw new RuntimeException("Stock update failed for SKU: " + sku + " - " + e.getMessage(), e);
        }
    }
    /**
     * Get current stock level for a product by summing all stock movements
     */
    public BigDecimal getCurrentStockLevel(Long productId, Long userId) {
        try {
            // Use the user-specific query if available, otherwise fallback to general query
            BigDecimal stockLevel = stockRepo.sumByProductIdAndUserId(productId, userId);
            return stockLevel != null ? stockLevel : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to calculate stock level for product {}: {}", productId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Get current stock level for a product using native query (fallback)
     */

    /**
     * Helper method to calculate total from items
     */
    private BigDecimal calculateTotal(List<TillItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (TillItem it : items) {
            BigDecimal unit = safe(BigDecimal.valueOf(it.getPrice()));
            int qty = it.getQty() == 0 ? 0 : it.getQty();
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(qty));
            total = total.add(lineTotal);
        }
        return total;
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}