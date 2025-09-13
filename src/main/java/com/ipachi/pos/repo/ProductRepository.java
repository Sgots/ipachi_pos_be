package com.ipachi.pos.repo;

// src/main/java/com/ipachi/pos/inventory/repo/ProductRepository.java

import com.ipachi.pos.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
    boolean existsByBarcodeIgnoreCase(String barcode);
    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);

    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
            String name, String sku, String barcode, Pageable pageable);
}
