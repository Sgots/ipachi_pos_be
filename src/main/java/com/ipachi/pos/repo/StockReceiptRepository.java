package com.ipachi.pos.repo;

import com.ipachi.pos.model.StockReceipt;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {

    Optional<StockReceipt> findByIdAndBusinessId(Long id, Long businessId);

    // Latest 50 receipts for a business
    List<StockReceipt> findTop50ByBusinessIdOrderByCreatedAtDesc(Long businessId);

    // Case-insensitive search within a business (label or filename)
    @Query("""
           SELECT r FROM StockReceipt r
           WHERE r.businessId = :businessId
             AND (
                  LOWER(r.label)    LIKE CONCAT('%', :q, '%')
               OR LOWER(r.fileName) LIKE CONCAT('%', :q, '%')
             )
           ORDER BY r.createdAt DESC
           """)
    List<StockReceipt> searchByBusinessAndQuery(@Param("businessId") Long businessId,
                                                @Param("q") String q);
}
