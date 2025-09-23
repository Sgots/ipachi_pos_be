package com.ipachi.pos.service;

import com.ipachi.pos.dto.CheckoutRequest;
import com.ipachi.pos.dto.TillItem;
import com.ipachi.pos.model.Product;
import com.ipachi.pos.model.StockMovement;
import com.ipachi.pos.model.Transaction;
import com.ipachi.pos.model.TransactionLine;
import com.ipachi.pos.repo.*;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.ipachi.pos.tax.TaxCalculator.line;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final TransactionRepository txRepo;
    private final TransactionLineRepository lineRepo;
    private final ProductRepository productRepo;
    private final StockMovementRepository stockRepo;
    private final CurrentRequest ctx;

    private final SettingsRepository settingsRepo;

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

    /**
     * Persist a transaction and its line items, update inventory levels,
     * and return the saved Transaction (with generated ID and total).
     */
    @Transactional
    public Transaction checkout(CheckoutRequest req) {
        Long businessId = biz();
        Long userId = user();

        var items = (req.getItems() == null) ? List.<TillItem>of() : req.getItems();
        log.info("Processing checkout for businessId: {}, userId: {}, items: {}", businessId, userId, items.size());

        // VAT settings
        var st = settingsRepo.findByBusinessId(businessId).orElse(null);
        boolean enableVat        = st != null && st.isEnableVat();
        boolean pricesIncludeVat = st != null && st.isPricesIncludeVat();
        BigDecimal ratePct       = (st != null && st.getVatRate() != null) ? st.getVatRate() : BigDecimal.ZERO;

        // Pre-scale zero for NOT NULL columns
        BigDecimal ZERO2 = BigDecimal.ZERO.setScale(2);

        // 1) Create tx head with zeroed totals (avoids NOT NULL violations on first insert)
        Transaction tx = Transaction.builder()
                .customerName((req.getCustomerName() != null && !req.getCustomerName().isBlank())
                        ? req.getCustomerName() : "Walk-in")
                .businessId(businessId)
                .createdByUserId(userId)
                .userId(userId)
                .terminalId(term())
                .subtotalNet(ZERO2)   // <-- important
                .totalVat(ZERO2)      // <-- important
                .totalGross(ZERO2)    // <-- important
                .total(ZERO2)         // legacy/overall total aligned to gross
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        tx = txRepo.save(tx); // safe: all NOT NULL money columns are set

        // 2) Accumulators
        BigDecimal sumNet   = ZERO2;
        BigDecimal sumVat   = ZERO2;
        BigDecimal sumGross = ZERO2;

        List<TransactionLine> savedLines = new ArrayList<>();

        for (TillItem item : items) {
            // price & qty
            BigDecimal unit = safe(BigDecimal.valueOf(item.getPrice())).setScale(2, RoundingMode.HALF_UP);
            int qty = Math.max(0, item.getQty());

            // Resolve product (for profit & stock)
            var product = productRepo.findBySkuIgnoreCaseAndBusinessId(item.getSku().trim(), businessId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Product not found for SKU in this business"));

            BigDecimal buy = safe(product.getBuyPrice());
            BigDecimal lineBase = unit.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

            // VAT breakdown (your TaxCalculator is fine)
            var breakdown = (enableVat)
                    ? line(unit, qty, pricesIncludeVat, ratePct) // net, vat, gross
                    : new com.ipachi.pos.tax.TaxCalculator.Breakdown(lineBase, ZERO2, lineBase);

            // profit based on NET (sell net - buy)
            BigDecimal unitNet = breakdown.net().divide(BigDecimal.valueOf(Math.max(qty, 1)), 2, RoundingMode.HALF_UP);
            BigDecimal profit  = unitNet.subtract(buy).multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

            TransactionLine line = TransactionLine.builder()
                    .transaction(tx)
                    .sku(item.getSku())
                    .name(item.getName() != null ? item.getName() : item.getSku())
                    .unitPrice(unit)
                    .qty(qty)
                    .lineTotal(lineBase)          // legacy
                    .netAmount(breakdown.net())
                    .vatAmount(breakdown.vat())
                    .grossAmount(breakdown.gross())
                    .vatRateApplied(enableVat ? ratePct : BigDecimal.ZERO)
                    .profit(profit)
                    .remainingStock(BigDecimal.ZERO)
                    .businessId(businessId)
                    .createdByUserId(userId)
                    .userId(userId)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            TransactionLine saved = lineRepo.save(line);

            if (qty > 0) {
                updateStockForSale(item.getSku(), qty, "TX-" + tx.getId(), businessId, userId);
                BigDecimal after = getCurrentStockLevel(product.getId(), businessId);
                saved.setRemainingStock(after);
                saved.setUpdatedAt(OffsetDateTime.now());
                saved = lineRepo.save(saved);
            }

            sumNet   = sumNet.add(breakdown.net()).setScale(2, RoundingMode.HALF_UP);
            sumVat   = sumVat.add(breakdown.vat()).setScale(2, RoundingMode.HALF_UP);
            sumGross = sumGross.add(breakdown.gross()).setScale(2, RoundingMode.HALF_UP);

            savedLines.add(saved);
        }

        // 3) Update header totals and save again
        tx.setSubtotalNet(sumNet);
        tx.setTotalVat(sumVat);
        tx.setTotalGross(sumGross);
        tx.setTotal(sumGross); // keep legacy total aligned with gross

        tx.setUpdatedAt(OffsetDateTime.now());
        tx = txRepo.save(tx);

        log.info("Checkout complete - txId: {}, net: {}, vat: {}, gross: {}, lines: {}, business: {}, user: {}",
                tx.getId(), tx.getSubtotalNet(), tx.getTotalVat(), tx.getTotalGross(),
                savedLines.size(), businessId, userId);

        return tx;
    }

    // ... updateStockForSale(), getCurrentStockLevel() unchanged ...

    /** Create & save a transaction line, then deduct stock via negative movement. */
    /** Create & save a transaction line, then deduct stock via negative movement. */
    private TransactionLine createAndSaveLineItem(Transaction tx, TillItem item, Long businessId, Long userId) {
                BigDecimal unit = safe(BigDecimal.valueOf(item.getPrice()));
                int qty = Math.max(0, item.getQty());
                BigDecimal qtyBD = BigDecimal.valueOf(qty);
                BigDecimal lineTotal = unit.multiply(qtyBD);
        
                        // Resolve product to compute profit and to read stock later
                                var product = productRepo.findBySkuIgnoreCaseAndBusinessId(item.getSku().trim(), businessId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                        "Product not found for SKU in this business"));
                BigDecimal buy = safe(product.getBuyPrice());
                BigDecimal profit = unit.subtract(buy).multiply(qtyBD);

        TransactionLine line = TransactionLine.builder()
                .transaction(tx)
                .sku(item.getSku())
                .name(item.getName() != null ? item.getName() : item.getSku())
                .unitPrice(unit)
                .qty(qty)
                .lineTotal(lineTotal)
                                .profit(profit)
                                .remainingStock(BigDecimal.ZERO) // will be updated after stock movement
                .businessId(businessId)         // owner
                .createdByUserId(userId)        // who added the line
                .userId(userId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

                TransactionLine savedLine = lineRepo.save(line);

        if (qty > 0) {
            updateStockForSale(item.getSku(), qty, "TX-" + tx.getId(), businessId, userId);
                        // compute remaining stock after deduction
                                BigDecimal after = getCurrentStockLevel(product.getId(), businessId);
                        savedLine.setRemainingStock(after);
                        savedLine.setUpdatedAt(OffsetDateTime.now());
                        savedLine = lineRepo.save(savedLine);
        }

        return savedLine;
    }

    /** Create a negative stock movement for a sale (business-scoped). */
    @Transactional
    public void updateStockForSale(String sku, int quantityToDeduct, String reference, Long businessId, Long userId) {
        if (sku == null || sku.isBlank() || quantityToDeduct <= 0) {
            log.debug("Skipping stock update (invalid input) sku={}, qty={}", sku, quantityToDeduct);
            return;
        }

        // 1) Resolve product in this business
        Product product = productRepo.findBySkuIgnoreCaseAndBusinessId(sku.trim(), businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found for SKU in this business"));

        // 2) Check current stock for this business
        BigDecimal current = getCurrentStockLevel(product.getId(), businessId);
        if (current.compareTo(BigDecimal.valueOf(quantityToDeduct)) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient stock for %s: requested %d, available %.4f"
                            .formatted(product.getName(), quantityToDeduct, current));
        }

        // 3) Record sale movement (negative delta) with audit stamps
        StockMovement mv = StockMovement.builder()
                .businessId(businessId)
                .createdByUserId(userId)
                .userId(userId)
                .terminalId(term())
                .product(product)
                .quantityDelta(BigDecimal.valueOf(quantityToDeduct).negate())
                .note("Sale - " +  reference)
                .createdAt(OffsetDateTime.now())
                .build();

        stockRepo.saveAndFlush(mv);

        BigDecimal after = getCurrentStockLevel(product.getId(), businessId);
        log.info("Stock updated (sale) sku={}, before={}, after={}, deducted={}, biz={}",
                sku, current, after, quantityToDeduct, businessId);
    }

    /** Sum movements for a product within a business. */
    public BigDecimal getCurrentStockLevel(Long productId, Long businessId) {
        BigDecimal v = stockRepo.sumByProductIdAndBusinessId(productId, businessId);
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal calculateTotal(List<TillItem> items) {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (TillItem it : items) {
            BigDecimal unit = safe(BigDecimal.valueOf(it.getPrice()));
            int qty = Math.max(0, it.getQty());
            total = total.add(unit.multiply(BigDecimal.valueOf(qty)));
        }
        return total;
    }

    private static BigDecimal safe(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
