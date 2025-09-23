// src/main/java/com/ipachi/pos/service/ReportsService.java
package com.ipachi.pos.service;

import com.ipachi.pos.dto.reports.*;
import com.ipachi.pos.model.Product;
import com.ipachi.pos.repo.*;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportsService {

    private final CurrentRequest ctx;
    private final TransactionRepository txRepo;
    private final TransactionLineRepository lineRepo;
    private final ProductRepository productRepo;
    private final StockMovementRepository stockRepo;

    private Long biz() {
        Long v = ctx.getBusinessId();
        if (v == null) throw new IllegalStateException("X-Business-Id missing");
        return v;
    }

    private static OffsetDateTime startOfDay(LocalDate d) {
        return d.atStartOfDay().atOffset(ZoneOffset.UTC);
    }
    private static OffsetDateTime endOfDay(LocalDate d) {
        return d.plusDays(1).atStartOfDay().minusNanos(1).atOffset(ZoneOffset.UTC);
    }

    public DashboardKpis dashboard(OffsetDateTime start, OffsetDateTime end) {
        Long b = biz();
        long customers = txRepo.countByBusinessIdAndCreatedAtBetween(b, start, end);
        BigDecimal totalSales = nz(txRepo.sumTotal(b, start, end));
        BigDecimal overallProfit = nz(lineRepo.sumProfit(b, start, end));

        List<ProductSalesRow> perProduct = lineRepo.salesByProduct(b, start, end);
        String topProduct = perProduct.isEmpty() ? "-" : perProduct.get(0).getName();

        return new DashboardKpis(customers, totalSales, overallProfit, topProduct);
    }

    public List<ProductSalesRow> salesByProduct(OffsetDateTime start, OffsetDateTime end) {
        return lineRepo.salesByProduct(biz(), start, end);
    }

    public List<MonthlySalesRow> monthlyTrend(OffsetDateTime start, OffsetDateTime end) {
        Long b = biz();
        return txRepo.monthlySales(b, start, end).stream()
                .map(a -> new MonthlySalesRow((String)a[0], toBig(a[1])))
                .toList();
    }

    public List<ProductSalesRow> bestPerformers(OffsetDateTime start, OffsetDateTime end, int topN) {
        return lineRepo.salesByProduct(biz(), start, end).stream()
                .sorted(Comparator.comparing(ProductSalesRow::getTotal).reversed())
                .limit(topN)
                .toList();
    }

    public List<CategoryProfitRow> profitByCategory(OffsetDateTime start, OffsetDateTime end) {
        return lineRepo.profitByCategory(biz(), start, end);
    }



    /** Cash-Up: per product — cash (buying price total) + profit. */
    public List<CashUpRow> cashUpRows(OffsetDateTime start, OffsetDateTime end) {
        Long b = biz();
        // reuse salesByProduct to get product names, then compute cash & profit via JPQL in memory (simple way)
        List<Product> products = productRepo.findByBusinessId(b);
        var perProduct = lineRepo.salesByProduct(b, start, end);
        return perProduct.stream().map(row -> {
            Product p = products.stream()
                    .filter(pp -> pp.getSku().equalsIgnoreCase(row.getSku()))
                    .findFirst().orElse(null);
            BigDecimal buy = p == null ? BigDecimal.ZERO : nz(p.getBuyPrice());
            // cash = buy * (Σ qty). We need Σ qty for that SKU
            // quick JPQL alternative: compute Σ qty by (total / avgUnitPrice) — but better to query directly.
            // To keep it simple & accurate, add another query or assume unitPrice constant per SKU in period.
            // We’ll estimate qty = totalSales / p.sellPrice if sellPrice > 0; else qty=0.
            BigDecimal qtyEstimate = BigDecimal.ZERO;
            if (p != null && nz(p.getSellPrice()).compareTo(BigDecimal.ZERO) > 0) {
                qtyEstimate = safeDiv(row.getTotal(), p.getSellPrice());
            }
            BigDecimal cash = buy.multiply(qtyEstimate);
            // profit estimate = (sell - buy) * qty
            BigDecimal profit = nz(p == null ? BigDecimal.ZERO : p.getSellPrice().subtract(buy)).multiply(qtyEstimate);
            return new CashUpRow(row.getName(), cash, profit);
        }).toList();
    }

    public CashUpTotals cashUpTotals(List<CashUpRow> rows) {
        BigDecimal totalCash = rows.stream().map(CashUpRow::getCash).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = rows.stream().map(CashUpRow::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CashUpTotals(totalCash, totalProfit, totalCash.add(totalProfit));
    }

    /** Trade Account Statement. */
    @Transactional(readOnly = true)
    public TradeAccountStatement tradeAccount(OffsetDateTime start, OffsetDateTime end) {
        Long b = biz();
        BigDecimal sales = nz(txRepo.sumTotal(b, start, end));

        // Opening stock value (value at start minus 1ns)
        OffsetDateTime justBeforeStart = start.minusNanos(1);
        BigDecimal opening = stockValueAt(b, justBeforeStart);

        // Purchases during period
        BigDecimal newStock = nz(stockRepo.purchasesValue(b, start, end));

        // Closing stock value at end
        BigDecimal closing = stockValueAt(b, end);

        BigDecimal costOfSales = opening.add(newStock).subtract(closing);
        BigDecimal gross = sales.subtract(costOfSales);

        return new TradeAccountStatement(sales, opening, newStock, closing, costOfSales, gross);
    }

    private BigDecimal stockValueAt(Long biz, OffsetDateTime ts) {
        List<Product> products = productRepo.findByBusinessId(biz);
        BigDecimal total = BigDecimal.ZERO;
        for (Product p : products) {
            BigDecimal qty = nz(stockRepo.sumQtyUpTo(p.getId(), biz, ts));
            if (qty.signum() > 0) {
                total = total.add(qty.multiply(nz(p.getBuyPrice())));
            }
        }
        return total;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static BigDecimal toBig(Object v) { return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString()); }
    private static BigDecimal safeDiv(BigDecimal a, BigDecimal b) {
        if (b == null || b.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return a.divide(b, 6, java.math.RoundingMode.HALF_UP);
    }

    // helpers to compute default comprehensive range (YTD)
    public static OffsetDateTime ytdStart() {
        LocalDate jan1 = LocalDate.now().withDayOfYear(1);
        return startOfDay(jan1);
    }
    public static OffsetDateTime nowUtc() { return OffsetDateTime.now(ZoneOffset.UTC); }

    public List<LocationSalesRow> salesByLocation(OffsetDateTime start, OffsetDateTime end) {
        return txRepo.salesByLocation(biz(), start, end).stream()
                .map(a -> new LocationSalesRow(
                        a[0] == null ? "Unknown" : String.valueOf(a[0]),
                        a[1] == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(a[1]))
                ))
                .toList();
    }
}
