package com.ipachi.pos.repo;

import com.ipachi.pos.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.userId = :userId")
    Optional<Product> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.id = :id AND p.userId = :userId")
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    // NEW: Find product by SKU (case insensitive)
    @Query("SELECT p FROM Product p WHERE LOWER(p.sku) = LOWER(:sku) AND p.userId = :userId")
    Optional<Product> findBySkuIgnoreCaseAndUserId(@Param("sku") String sku, @Param("userId") Long userId);

    // Alternative: Find by exact SKU match (if you prefer case sensitive)
    @Query("SELECT p FROM Product p WHERE p.sku = :sku AND p.userId = :userId")
    Optional<Product> findBySkuAndUserId(@Param("sku") String sku, @Param("userId") Long userId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE LOWER(p.sku) = LOWER(:sku) AND p.userId = :userId")
    boolean existsBySkuIgnoreCaseAndUserId(@Param("sku") String sku, @Param("userId") Long userId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE LOWER(p.sku) = LOWER(:sku) AND p.userId = :userId AND p.id != :id")
    boolean existsBySkuIgnoreCaseAndUserIdAndIdNot(@Param("sku") String sku, @Param("userId") Long userId, @Param("id") Long id);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE LOWER(p.barcode) = LOWER(:barcode) AND p.userId = :userId")
    boolean existsByBarcodeIgnoreCaseAndUserId(@Param("barcode") String barcode, @Param("userId") Long userId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE LOWER(p.barcode) = LOWER(:barcode) AND p.userId = :userId AND p.id != :id")
    boolean existsByBarcodeIgnoreCaseAndUserIdAndIdNot(@Param("barcode") String barcode, @Param("userId") Long userId, @Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.userId = :userId")
    Page<Product> findByUserId(@Param("userId") Long userId, Pageable pageable);

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

    List<Product> findByUserId(Long userId);
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
    boolean existsByBarcodeIgnoreCase(String barcode);
    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);

    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
            String name, String sku, String barcode, Pageable pageable);
}