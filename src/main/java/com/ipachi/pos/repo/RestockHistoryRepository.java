// src/main/java/com/ipachi/pos/repo/RestockHistoryRepository.java
package com.ipachi.pos.repo;

import com.ipachi.pos.dto.ReceiptItemView;
import com.ipachi.pos.dto.RestockHistoryView;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class RestockHistoryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Dates removed: opening/new/closing are computed per receipt timestamp.
     * - Opening: Σ(GREATEST(qty_delta, 0) * buy_price) for ALL movements before r.receipt_at
     * - New:     Σ(GREATEST(qty_delta, 0) * buy_price) for movements in THIS receipt
     * - Closing: opening + new
     * Negatives are ignored everywhere.
     */
    public List<RestockHistoryView> restockHistory(Long biz, OffsetDateTime from, OffsetDateTime to, String q) {
        String sql = """
        SELECT
            r.id                                   AS receipt_id,
            r.receipt_at                           AS receipt_at,
            r.label                                AS label,
            u.username                             AS uploaded_by,
            (CASE WHEN r.file_size IS NOT NULL AND r.file_size > 0 THEN 1 ELSE 0 END) AS has_file,
            CONCAT('/api/inventory/receipts/', r.id, '/file')                          AS file_url,

            /* OPENING: total value at the time of this receipt (ignore negatives) */
            COALESCE((
                SELECT SUM(GREATEST(sm.quantity_delta, 0) * p.buy_price)
                FROM inv_stock_movements sm
                JOIN inv_products p ON p.id = sm.product_id
                WHERE sm.business_id = :biz
                  AND sm.created_at < r.receipt_at
            ), 0) AS opening_value,

            /* NEW (per receipt): value of positive adds in this receipt */
            COALESCE((
                SELECT SUM(GREATEST(sm.quantity_delta, 0) * p.buy_price)
                FROM inv_stock_movements sm
                JOIN inv_products p ON p.id = sm.product_id
                WHERE sm.business_id = :biz
                  AND sm.receipt_id = r.id
                  AND sm.quantity_delta > 0
            ), 0) AS new_value_per_receipt

        FROM inv_stock_receipt r
        JOIN users u ON u.user_id = r.user_id
        WHERE r.business_id = :biz
          AND ( :q IS NULL OR :q = '' OR
                LOWER(r.label)     LIKE CONCAT('%', LOWER(:q), '%') OR
                LOWER(r.file_name) LIKE CONCAT('%', LOWER(:q), '%')
              )
        ORDER BY r.receipt_at DESC, r.created_at DESC
        LIMIT 200
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("biz", biz);
        query.setParameter("q", q);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<RestockHistoryView> out = new ArrayList<>();
        for (Object[] r : rows) {
            Long id = ((Number) r[0]).longValue();
            Timestamp ts = (Timestamp) r[1];
            String label = (String) r[2];
            String uploadedBy = (String) r[3];
            boolean hasFile = ((Number) r[4]).intValue() == 1;
            String fileUrl = (String) r[5];

            BigDecimal opening = (BigDecimal) r[6];
            BigDecimal addedThisReceipt = (BigDecimal) r[7];

            // CLOSING = opening at receipt time + new added in that receipt
            BigDecimal closing = (opening == null ? BigDecimal.ZERO : opening)
                    .add(addedThisReceipt == null ? BigDecimal.ZERO : addedThisReceipt);

            out.add(new RestockHistoryView(
                    id,
                    ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC),
                    label,
                    uploadedBy,
                    hasFile,
                    fileUrl,
                    opening,              // opening stock value at receipt time
                    addedThisReceipt,     // new stock value added by THIS receipt (buy price, positives only)
                    closing               // closing = opening + new
            ));
        }
        return out;
    }

    public List<ReceiptItemView> receiptItems(Long biz, Long receiptId) {
        String sql = """
        SELECT
            p.id,
            p.sku,
            p.name,
            SUM(sm.quantity_delta)                          AS qty,
            p.buy_price                                     AS unit_price,
            SUM(sm.quantity_delta) * p.buy_price            AS value
        FROM inv_stock_movements sm
        JOIN inv_products p ON p.id = sm.product_id
        WHERE sm.business_id = :biz
          AND sm.receipt_id = :receiptId
          AND sm.quantity_delta > 0           -- ignore negatives
        GROUP BY p.id, p.sku, p.name, p.buy_price
        ORDER BY p.name
        """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("biz", biz);
        query.setParameter("receiptId", receiptId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<ReceiptItemView> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new ReceiptItemView(
                    ((Number) r[0]).longValue(),
                    (String) r[1],
                    (String) r[2],
                    (BigDecimal) r[3],
                    (BigDecimal) r[4],  // buy price
                    (BigDecimal) r[5]   // qty * buy price
            ));
        }
        return out;
    }
}
