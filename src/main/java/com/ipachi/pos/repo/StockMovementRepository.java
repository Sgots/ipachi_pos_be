package com.ipachi.pos.repo;

import com.ipachi.pos.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    @Query("""
      select coalesce(sum(m.quantityDelta), 0)
      from StockMovement m
      where m.product.id = :productId and m.businessId = :biz and m.createdAt <= :ts
    """)
    BigDecimal sumQtyUpTo(Long productId, Long biz, OffsetDateTime ts);

    // purchases in period (positive deltas × buyPrice) aggregated over all products
    @Query("""
      select coalesce(sum( case when m.quantityDelta > 0 then m.quantityDelta * p.buyPrice else 0 end ), 0)
      from StockMovement m
      join m.product p
      where m.businessId = :biz and m.createdAt between :start and :end
    """)
    BigDecimal purchasesValue(Long biz, OffsetDateTime start, OffsetDateTime end);

    /* ========= AGGREGATIONS (BUSINESS SCOPED) ========= */

    /**
     * Sum quantity for a specific product within a business.
     */
    @Query("""
           SELECT COALESCE(SUM(m.quantityDelta), 0)
           FROM StockMovement m
           WHERE m.product.id = :productId AND m.businessId = :businessId
           """)
    BigDecimal sumByProductIdAndBusinessId(@Param("productId") Long productId,
                                           @Param("businessId") Long businessId);

    /**
     * Bulk totals for all products within a business (native for speed).
     * Returns rows: [product_id, qty]
     */
    @Query(value = """
        SELECT p.id AS product_id, COALESCE(SUM(sm.quantity_delta), 0) AS qty
        FROM inv_stock_movements sm
        JOIN inv_products p ON sm.product_id = p.id
        WHERE sm.business_id = :businessId
        GROUP BY p.id
        """, nativeQuery = true)
    List<Object[]> totalsByProductIdAndBusinessId(@Param("businessId") Long businessId);

    /* ========= LOOKUPS (BUSINESS SCOPED) ========= */

    /**
     * Recent movements for a product identified by SKU within a business.
     */
    @Query("""
        SELECT m FROM StockMovement m
        JOIN m.product p
        WHERE p.sku = :sku AND m.businessId = :businessId
        ORDER BY m.createdAt DESC
        """)
    List<StockMovement> findRecentByProductSkuAndBusinessId(@Param("sku") String sku,
                                                            @Param("businessId") Long businessId,
                                                            Pageable pageable);

    /* ========= LEGACY (USER-SCOPED) — keep temporarily if needed ========= */
    // Prefer the business-scoped methods above. These can be deleted after refactor.
    @Deprecated
    @Query("select coalesce(sum(m.quantityDelta),0) from StockMovement m where m.product.id = :productId")
    BigDecimal sumByProductId(@Param("productId") Long productId);

    @Deprecated
    @Query("select m.product.id, coalesce(sum(m.quantityDelta),0) from StockMovement m group by m.product.id")
    List<Object[]> totalsByProduct();

    // Basic accessors (still okay)
    List<StockMovement> findByBusinessId(Long businessId);
    Optional<StockMovement> findByIdAndBusinessId(Long id, Long businessId);
}
