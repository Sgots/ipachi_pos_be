// src/main/java/com/ipachi/pos/repo/StockReceiptRepository.java
package com.ipachi.pos.repo;

import com.ipachi.pos.model.StockReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {
    List<StockReceipt> findTop50ByOrderByCreatedAtDesc();

    @Query("""
        select r from StockReceipt r
         where lower(r.label) like concat('%', :q, '%')
            or lower(r.fileName) like concat('%', :q, '%')
         order by r.createdAt desc
    """)
    List<StockReceipt> search(String q);
}
