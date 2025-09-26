package com.ipachi.pos.repo;

import com.ipachi.pos.model.StockReceipt;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {
    Optional<StockReceipt> findByIdAndBusinessId(Long id, Long businessId);

    // Top 50, newest by receipt date (fallback to createdAt if equal)
    List<StockReceipt> findTop50ByBusinessIdOrderByReceiptAtDescCreatedAtDesc(Long businessId);

    @Query("""
                select r from StockReceipt r
                where r.businessId = :biz
                  and (
                    :q is null or :q = '' or
                    lower(r.label) like concat('%', :q, '%') or
                    lower(r.fileName) like concat('%', :q, '%')
                  )
                order by r.receiptAt desc, r.createdAt desc
            """)
    List<StockReceipt> searchByBusinessAndQuery(@Param("biz") Long businessId, @Param("q") String q);


    // Latest 50 receipts for a business

    // Case-insensitive search within a business (label or filename)
}
