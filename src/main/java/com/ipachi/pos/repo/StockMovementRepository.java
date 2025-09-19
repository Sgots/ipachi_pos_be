package com.ipachi.pos.repo;

import com.ipachi.pos.model.Product;
import com.ipachi.pos.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    @Query("SELECT p FROM Product p WHERE p.userId = :userId ORDER BY p.name ASC")
    List<Product> findByUserIdOrderByNameAsc(@Param("userId") Long userId);

    @Query("SELECT p FROM Product p WHERE p.userId = :userId AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :sku, '%')) OR " +
            "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :barcode, '%')))")
    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrBarcodeContainingIgnoreCaseAndUserId(
            @Param("name") String name,
            @Param("sku") String sku,
            @Param("barcode") String barcode,
            @Param("userId") Long userId,
            Pageable pageable
    );

    /**
     * Sum quantity for a specific product and user (NEW: with userId filter)
     */
    @Query("SELECT COALESCE(SUM(m.quantityDelta), 0) FROM StockMovement m " +
            "JOIN m.product p WHERE p.id = :productId AND m.userId = :userId")
    BigDecimal sumByProductIdAndUserId(@Param("productId") Long productId, @Param("userId") Long userId);

    /**
     * Alternative native query for stock calculation
     */
    @Query(value = """
        SELECT COALESCE(SUM(sm.quantity_delta), 0) 
        FROM inv_stock_movements sm 
        JOIN inv_products p ON sm.product_id = p.id 
        WHERE sm.product_id = :productId AND p.user_id = :userId
        """, nativeQuery = true)
    BigDecimal sumStockByProductIdNative(@Param("productId") Long productId, @Param("userId") Long userId);

    List<StockMovement> findByUserId(Long userId);
    Optional<StockMovement> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(sum(m.quantityDelta),0) " +
            "from StockMovement m where m.product.id = :productId")
    BigDecimal sumByProductId(@Param("productId") Long productId);

    @Query("select m.product.id, coalesce(sum(m.quantityDelta),0) " +
            "from StockMovement m group by m.product.id")
    List<Object[]> totalsByProduct();

    @Query(value = """
        SELECT p.id AS product_id, COALESCE(SUM(sm.quantity_delta), 0) AS qty
        FROM inv_stock_movements sm
        JOIN inv_products p ON sm.product_id = p.id
        WHERE p.user_id = :userId
        GROUP BY p.id
        """, nativeQuery = true)
    List<Object[]> totalsByProductIdAndUserId(@Param("userId") Long userId);

    // NEW: Find recent movements for a product
    @Query("SELECT m FROM StockMovement m JOIN m.product p WHERE p.sku = :sku AND m.userId = :userId ORDER BY m.createdAt DESC")
    List<StockMovement> findRecentByProductSku(@Param("sku") String sku, @Param("userId") Long userId, Pageable pageable);
}