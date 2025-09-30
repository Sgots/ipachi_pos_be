package com.ipachi.pos.sim;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SeedService {

    private final JdbcTemplate jdbc;

    public SeedService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** VAT = 10%; prices_include_vat = 1 → sell_price is GROSS */
    private static final BigDecimal VAT_RATE = new BigDecimal("0.10");
    private static final BigDecimal ONE = new BigDecimal("1.00");

    /** Use existing users */
    private static final long USER_ADMIN = 5L;   // receipts / stock-ins / adjustments
    private static final long USER_STAFF = 6L;   // POS sales (stock-outs)

    /** Fixed product set (must already exist and belong to the business) */
    private static final List<Long> PRODUCT_IDS = List.of(4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);

    /** Optional cols present in inv_stock_movements (detected at runtime) */
    private Boolean smHasCreatedBy = null;
    private Boolean smHasUpdatedBy = null;
    private Boolean smHasUserId    = null;

    /** Optional cols present in tx_head (detected at runtime) */
    private Boolean thHasUserId        = null;
    private Boolean thHasSubtotalVat   = null;
    private Boolean thHasSubtotalGross = null;
    private Boolean thHasTotalNet      = null;
    private Boolean thHasTotalVat      = null;
    private Boolean thHasTotalGross    = null;
    private Boolean thHasCustomerName  = null;

    /** Run on demand: simulate last 3 months, ≤ maxPerMonth transactions per month, with receipts (BLOBs).
     *  STRONG GUARANTEE: Never insert a negative stock movement that would push quantity below zero.
     *  We check DB quantity up to the sale time and auto-restock (with a dated receipt) if needed. */
    public Map<String, Object> run(Long businessId, int maxPerMonth) {
        initColumnFlags();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("businessId", businessId);
        out.put("maxPerMonth", maxPerMonth);

        // Load products and prices
        Map<Long, Product> products = loadProducts(businessId, PRODUCT_IDS);
        if (products.size() != PRODUCT_IDS.size()) {
            out.put("status", "error");
            out.put("message", "Some products not found under business " + businessId + ". Found: " + products.keySet());
            return out;
        }

        // Running stock per product from DB (for 'remaining_stock' on tx_line); DB checks still authoritative
        Map<Long, BigDecimal> running = currentStockForProducts(businessId, PRODUCT_IDS);

        YearMonth now = YearMonth.now(ZoneId.of("Africa/Gaborone"));
        List<YearMonth> months = List.of(now.minusMonths(2), now.minusMonths(1), now);

        Random rnd = new Random();

        // Pre-window top-up for any product ≤ 0 (CREATE RECEIPT + link), modest quantities
        LocalDateTime preWindow = months.get(0).atDay(1).atStartOfDay().minusDays(3);
        boolean didInitial = false;
        if (PRODUCT_IDS.stream().anyMatch(pid -> running.getOrDefault(pid, BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0)) {
            long rid = createReceipt(businessId, "Pre-window top-up", preWindow);
            for (Long pid : PRODUCT_IDS) {
                if (running.getOrDefault(pid, BigDecimal.ZERO).compareTo(BigDecimal.ZERO) <= 0) {
                    int qty = 20 + rnd.nextInt(21); // 20..40
                    BigDecimal qtyBD = q(qty);
                    stockMoveWithUser(businessId, pid, qtyBD, preWindow, rid, USER_ADMIN);
                    running.compute(pid, (k, v) -> (v == null ? BigDecimal.ZERO : v).add(qtyBD));
                    didInitial = true;
                }
            }
        }
        out.put("didInitialTopUp", didInitial);

        for (YearMonth ym : months) {
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();

            // 1) Periodic stock-ins (CREATE RECEIPTS + link) — FEWER and SMALLER to favor more negatives
            int restocks = 1 + rnd.nextInt(2); // 1..2
            for (int i = 0; i < restocks; i++) {
                LocalDateTime when = randomDateTime(monthStart, monthEnd);
                long rid = createReceipt(businessId, "Supplier GRN " + (i + 1) + " " + ym, when);
                int skuCount = 2 + rnd.nextInt(2); // 2..3 products
                for (Long pid : pickRandom(PRODUCT_IDS, skuCount, rnd)) {
                    int qty = 10 + rnd.nextInt(21); // 10..30
                    BigDecimal qtyBD = q(qty);
                    stockMoveWithUser(businessId, pid, qtyBD, when, rid, USER_ADMIN);
                    running.compute(pid, (k, v) -> v.add(qtyBD));
                }
            }

            // 2) Occasional adjustment stock-out (not a sale) — guard against underflow
            int outs = rnd.nextInt(2); // 0..1
            for (int i = 0; i < outs; i++) {
                Long pid = oneOf(PRODUCT_IDS, rnd);
                int qty = 1 + rnd.nextInt(2); // 1..2
                LocalDateTime when = randomDateTime(monthStart, monthEnd);

                // Ensure stock exists at 'when' before inserting negative movement
                BigDecimal need = q(qty);
                BigDecimal added = ensureStockAvailable(businessId, pid, when, need, "Adjustment " + (i + 1) + " " + ym);
                if (added.signum() > 0) running.compute(pid, (k, v) -> v.add(added));

                stockMoveWithUser(businessId, pid, need.negate(), when, null, USER_ADMIN);
                running.compute(pid, (k, v) -> v.subtract(need));
            }

            // 3) SALES — tx_head/tx_line and a NEGATIVE stock movement per line (USER_STAFF)
            int txCount = (maxPerMonth <= 0 ? 1 : (6 + rnd.nextInt(Math.max(1, maxPerMonth - 5)))); // 6..max
            for (int t = 0; t < txCount; t++) {
                LocalDateTime txAt = randomDateTime(monthStart, monthEnd);
                int lines = 2 + rnd.nextInt(3); // 2..4
                List<Long> picks = pickRandom(PRODUCT_IDS, lines, rnd);

                List<TxLine> built = new ArrayList<>();
                BigDecimal subtotalNet = BigDecimal.ZERO;
                BigDecimal subtotalVat = BigDecimal.ZERO;
                BigDecimal subtotalGross = BigDecimal.ZERO;

                for (Long pid : picks) {
                    Product p = products.get(pid);
                    int qty = 2 + rnd.nextInt(5); // 2..6
                    BigDecimal need = q(qty);

                    // Ensure stock exists at tx time in DB (hard guarantee)
                    BigDecimal added = ensureStockAvailable(businessId, pid, txAt, need, "Auto-replenish " + ym);
                    if (added.signum() > 0) running.compute(pid, (k, v) -> v.add(added));

                    // prices_include_vat=1 → sell_price is GROSS
                    BigDecimal unitGross = p.sell.setScale(2, RoundingMode.HALF_UP);
                    BigDecimal unitNet = div(unitGross, ONE.add(VAT_RATE));  // net = gross / 1.10
                    BigDecimal unitVat = unitGross.subtract(unitNet);

                    BigDecimal net = unitNet.multiply(need).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal vat = unitVat.multiply(need).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal gross = unitGross.multiply(need).setScale(2, RoundingMode.HALF_UP);

                    // Profit assumes buy_price is net
                    BigDecimal profit = unitNet.subtract(p.buy).multiply(need).setScale(2, RoundingMode.HALF_UP);

                    // update running to report 'remaining_stock' after this line
                    running.compute(pid, (k, v) -> v.subtract(need));
                    BigDecimal remaining = running.getOrDefault(pid, BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);

                    built.add(new TxLine(pid, p.name, p.sku, qty, unitNet, net, vat, gross, profit, remaining));
                    subtotalNet = subtotalNet.add(net);
                    subtotalVat = subtotalVat.add(vat);
                    subtotalGross = subtotalGross.add(gross);
                }

                Long txId = insertTxHead(businessId, txAt, subtotalNet, subtotalVat, subtotalGross);

                for (TxLine l : built) {
                    // persist tx line
                    insertTxLine(businessId, txId, txAt, l);
                    // record a NEGATIVE stock movement for the sale line (user = STAFF, no receipt)
                    stockMoveWithUser(businessId, l.productId, q(l.qty).negate(), txAt, null, USER_STAFF);
                }
            }
        }

        out.put("status", "ok");
        out.put("message", "Seeded stock movements with strict non-negative stock, receipts and transactions for the last 3 months.");
        return out;
    }

    /* ===================== DB helpers ===================== */

    private Map<Long, Product> loadProducts(Long biz, List<Long> ids) {
        String inSql = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = """
            select id, name, sku, buy_price, sell_price
            from inv_products
            where business_id = ? and id in (%s)
            """.formatted(inSql);

        List<Object> params = new ArrayList<>();
        params.add(biz);
        params.addAll(ids);

        Map<Long, Product> map = new HashMap<>();

        jdbc.query(sql, params.toArray(), (RowCallbackHandler) rs -> {
            long id = rs.getLong("id");
            map.put(id, new Product(
                    id,
                    rs.getString("name"),
                    rs.getString("sku"),
                    rs.getBigDecimal("buy_price"),
                    rs.getBigDecimal("sell_price")
            ));
        });

        return map;
    }

    private Map<Long, BigDecimal> currentStockForProducts(Long biz, List<Long> ids) {
        String inSql = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = """
            select product_id, coalesce(sum(quantity_delta), 0) as qty
            from inv_stock_movements
            where business_id = ? and product_id in (%s)
            group by product_id
            """.formatted(inSql);

        List<Object> params = new ArrayList<>();
        params.add(biz);
        params.addAll(ids);

        Map<Long, BigDecimal> map = new HashMap<>();
        for (Long id : ids) map.put(id, BigDecimal.ZERO);

        jdbc.query(sql, params.toArray(), (RowCallbackHandler) rs ->
                map.put(rs.getLong("product_id"), rs.getBigDecimal("qty"))
        );

        return map;
    }

    /** Quantity available for product at (<= when). */
    private BigDecimal qtyAt(Long biz, Long productId, LocalDateTime when) {
        BigDecimal v = jdbc.queryForObject("""
            select coalesce(sum(quantity_delta), 0)
            from inv_stock_movements
            where business_id = ? and product_id = ? and created_at <= ?
        """, BigDecimal.class, biz, productId, ts(when));
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Ensure at least 'need' is available at 'when'. If not, restock minimally (with receipt) earlier than 'when'. Returns quantity added. */
    private BigDecimal ensureStockAvailable(Long biz, Long productId, LocalDateTime when, BigDecimal need, String label) {
        BigDecimal available = qtyAt(biz, productId, when);
        if (available.compareTo(need) >= 0) return BigDecimal.ZERO;

        BigDecimal gap = need.subtract(available);                // how much we're short
        BigDecimal buffer = q(Math.max(3, need.intValue() / 2));  // small safety margin
        BigDecimal toAdd = gap.add(buffer);

        LocalDateTime topUpAt = when.minusMinutes(5);             // must be before the sale/adjustment
        long rid = createReceipt(biz, "Auto-ensure: " + label, topUpAt);
        stockMoveWithUser(biz, productId, toAdd, topUpAt, rid, USER_ADMIN);

        return toAdd;
    }

    /** Create a stock receipt row with RANDOM BLOB and return its id (safe even if id is not AUTO_INCREMENT). */
    private long createReceipt(Long biz, String label, LocalDateTime when) {
        long id = nextId("inv_stock_receipt");
        byte[] blob = randomBlob();
        long size = blob.length;

        String[] types = {"application/pdf", "image/jpeg", "image/png", "application/octet-stream"};
        String contentType = types[new Random().nextInt(types.length)];
        String ext = switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> ".bin";
        };
        String tsStr = when.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fileName = "receipt-" + tsStr + "-" + (1000 + new Random().nextInt(9000)) + ext;

        jdbc.update("""
            INSERT INTO inv_stock_receipt
              (business_id, created_at, file_size, id, receipt_at, updated_at, user_id, content_type, file_name, label, file_data)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
        """, biz, ts(when), size, id, ts(when), ts(when), USER_ADMIN, contentType, fileName, label, blob);

        return id;
    }

    /**
     * INSERT stock movement with optional receipt link and actor user.
     * Uses dynamic audit columns if present.
     */
    private void stockMoveWithUser(Long biz, Long productId, BigDecimal delta, LocalDateTime when, Long receiptId, long actorUserId) {
        StringBuilder cols = new StringBuilder("business_id, product_id, quantity_delta, created_at, updated_at");
        StringBuilder vals = new StringBuilder("?, ?, ?, ?, ?");
        List<Object> args = new ArrayList<>(List.of(biz, productId, delta, ts(when), ts(when)));

        if (Boolean.TRUE.equals(smHasCreatedBy)) {
            cols.append(", created_by_user_id");
            vals.append(", ?");
            args.add(actorUserId);
        }
        if (Boolean.TRUE.equals(smHasUpdatedBy)) {
            cols.append(", updated_by_user_id");
            vals.append(", ?");
            args.add(actorUserId);
        }
        if (Boolean.TRUE.equals(smHasUserId)) {
            cols.append(", user_id");
            vals.append(", ?");
            args.add(actorUserId);
        }

        cols.append(", receipt_id");
        if (receiptId == null) {
            vals.append(", NULL");
        } else {
            vals.append(", ?");
            args.add(receiptId);
        }

        String sql = "INSERT INTO inv_stock_movements (" + cols + ") VALUES (" + vals + ")";
        jdbc.update(sql, args.toArray());
    }

    /**
     * Insert tx_head with dynamic columns to satisfy NOT NULL constraints (total_gross, customer_name, etc.).
     * Always sets: business_id, created_at, updated_at, created_by_user_id, subtotal_net.
     * Optionally sets (if columns exist): user_id, subtotal_vat, subtotal_gross, total_net, total_vat, total_gross, customer_name.
     */
    private Long insertTxHead(Long biz, LocalDateTime when, BigDecimal subtotalNet, BigDecimal subtotalVat, BigDecimal subtotalGross) {
        StringBuilder cols = new StringBuilder("business_id, created_at, updated_at, created_by_user_id, subtotal_net");
        StringBuilder vals = new StringBuilder("?, ?, ?, ?, ?");
        List<Object> args = new ArrayList<>(List.of(biz, ts(when), ts(when), USER_STAFF, subtotalNet));

        if (Boolean.TRUE.equals(thHasUserId)) {
            cols.append(", user_id");
            vals.append(", ?");
            args.add(USER_STAFF);
        }
        if (Boolean.TRUE.equals(thHasSubtotalVat)) {
            cols.append(", subtotal_vat");
            vals.append(", ?");
            args.add(subtotalVat);
        }
        if (Boolean.TRUE.equals(thHasSubtotalGross)) {
            cols.append(", subtotal_gross");
            vals.append(", ?");
            args.add(subtotalGross);
        }
        if (Boolean.TRUE.equals(thHasTotalNet)) {
            cols.append(", total_net");
            vals.append(", ?");
            args.add(subtotalNet);
        }
        if (Boolean.TRUE.equals(thHasTotalVat)) {
            cols.append(", total_vat");
            vals.append(", ?");
            args.add(subtotalVat);
        }
        if (Boolean.TRUE.equals(thHasTotalGross)) {
            cols.append(", total_gross");
            vals.append(", ?");
            args.add(subtotalGross);
        }
        if (Boolean.TRUE.equals(thHasCustomerName)) {
            cols.append(", customer_name");
            vals.append(", ?");
            args.add("Walk-in");
        }

        String sql = "INSERT INTO tx_head (" + cols + ") VALUES (" + vals + ")";
        jdbc.update(sql, args.toArray());

        return jdbc.queryForObject("""
            select id from tx_head
            where business_id = ? and created_at = ?
            order by id desc limit 1
        """, Long.class, biz, ts(when));
    }

    /** Insert tx_line (do NOT specify 'id' column; let DB handle it) */
    private void insertTxLine(Long biz, Long txId, LocalDateTime when, TxLine l) {
        jdbc.update("""
            insert into tx_line
            (gross_amount, line_total, net_amount, profit, qty, remaining_stock, unit_price, vat_amount, vat_rate_applied,
             business_id, created_at, created_by_user_id, tx_id, updated_at, user_id, name, sku)
            values (?,?,?,?,?,?,?,?,?,
                    ?,?,?,?, ?, ?, ?, ?)
        """,
                l.gross,                          // gross_amount
                l.gross,                          // line_total (customer pays)
                l.net,                            // net_amount
                l.profit,                         // profit
                l.qty,                            // qty
                l.remaining == null ? q(0) : l.remaining, // remaining_stock (scale 4)
                l.unitNet,                        // unit_price (net/ex-VAT)
                l.vat,                            // vat_amount
                VAT_RATE.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP), // 10.00
                biz,
                ts(when),
                USER_STAFF,
                txId,
                ts(when),
                USER_STAFF,
                l.name,
                l.sku
        );
    }

    /* ===================== schema flags ===================== */

    private void initColumnFlags() {
        if (smHasCreatedBy == null) {
            smHasCreatedBy = columnExists("inv_stock_movements", "created_by_user_id");
        }
        if (smHasUpdatedBy == null) {
            smHasUpdatedBy = columnExists("inv_stock_movements", "updated_by_user_id");
        }
        if (smHasUserId == null) {
            smHasUserId = columnExists("inv_stock_movements", "user_id");
        }

        if (thHasUserId == null) {
            thHasUserId = columnExists("tx_head", "user_id");
        }
        if (thHasSubtotalVat == null) {
            thHasSubtotalVat = columnExists("tx_head", "subtotal_vat");
        }
        if (thHasSubtotalGross == null) {
            thHasSubtotalGross = columnExists("tx_head", "subtotal_gross");
        }
        if (thHasTotalNet == null) {
            thHasTotalNet = columnExists("tx_head", "total_net");
        }
        if (thHasTotalVat == null) {
            thHasTotalVat = columnExists("tx_head", "total_vat");
        }
        if (thHasTotalGross == null) {
            thHasTotalGross = columnExists("tx_head", "total_gross");
        }
        if (thHasCustomerName == null) {
            thHasCustomerName = columnExists("tx_head", "customer_name");
        }
    }

    private boolean columnExists(String table, String column) {
        Integer cnt = jdbc.queryForObject("""
            SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
        """, Integer.class, table, column);
        return cnt != null && cnt > 0;
    }

    /** Get next numeric id for a table that uses manual id allocation (good enough for seed data). */
    private long nextId(String table) {
        Long v = jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM " + table, Long.class);
        return v == null ? 1L : v;
    }

    /** Generate a random BLOB (4–32 KB) */
    private byte[] randomBlob() {
        SecureRandom sr = new SecureRandom();
        int size = 4096 + sr.nextInt(28 * 1024); // 4KB .. 32KB
        byte[] data = new byte[size];
        sr.nextBytes(data);
        return data;
    }

    /* ===================== utils ===================== */

    private static Timestamp ts(LocalDateTime ldt) {
        return Timestamp.valueOf(ldt);
    }

    /** money/amount scale(2) helper */
    private static BigDecimal bd(int v) {
        return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal bd(double v) {
        return new BigDecimal(String.valueOf(v)).setScale(2, RoundingMode.HALF_UP);
    }

    /** quantity scale(4) helper (to match remaining_stock DECIMAL(19,4)) */
    private static BigDecimal q(int v) {
        return new BigDecimal(v).setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal div(BigDecimal a, BigDecimal b) {
        return a.divide(b, 2, RoundingMode.HALF_UP);
    }

    private static LocalDateTime randomDateTime(LocalDate start, LocalDate end) {
        ZoneId zone = ZoneId.systemDefault();
        long startMs = start.atStartOfDay(zone).toInstant().toEpochMilli();
        long endMs = end.atTime(23, 59, 0).atZone(zone).toInstant().toEpochMilli();
        long r = ThreadLocalRandom.current().nextLong(startMs, endMs);
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(r), zone);
    }

    private static <T> List<T> pickRandom(List<T> src, int n, Random rnd) {
        List<T> copy = new ArrayList<>(src);
        Collections.shuffle(copy, rnd);
        return copy.subList(0, Math.min(n, copy.size()));
    }

    private static <T> T oneOf(List<T> src, Random rnd) {
        return src.get(rnd.nextInt(src.size()));
    }

    /* ===================== records ===================== */

    private record Product(long id, String name, String sku, BigDecimal buy, BigDecimal sell) {}

    private record TxLine(Long productId, String name, String sku, int qty,
                          BigDecimal unitNet, BigDecimal net, BigDecimal vat, BigDecimal gross,
                          BigDecimal profit, BigDecimal remaining) {}
}
