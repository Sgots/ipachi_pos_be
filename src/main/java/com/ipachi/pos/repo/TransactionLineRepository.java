package com.ipachi.pos.repo;


import com.ipachi.pos.dto.TxnLineDto;
import com.ipachi.pos.dto.reports.CategoryProfitRow;
import com.ipachi.pos.dto.reports.ProductSalesRow;
import com.ipachi.pos.model.TransactionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionLineRepository extends JpaRepository<TransactionLine, Long> {
    @Query("""
        select new com.ipachi.pos.dto.TxnLineDto(
            tl.transaction.id,
            tl.transaction.createdAt,
            tl.name,
            tl.sku,
            tl.qty,
            tl.lineTotal,
            tl.profit,
            tl.remainingStock
        )
        from TransactionLine tl
        where tl.businessId = :businessId
        order by tl.transaction.id desc, tl.id asc
    """)
    List<TxnLineDto> findLinesForBusiness(@Param("businessId") Long businessId);

    @Query("""
        select new com.ipachi.pos.dto.TxnLineDto(
            tl.transaction.id,
            tl.transaction.createdAt,
            tl.name,
            tl.sku,
            tl.qty,
            tl.lineTotal,
            tl.profit,
            tl.remainingStock
        )
        from TransactionLine tl
        where tl.businessId = :businessId
          and (:sku is null or lower(tl.sku) like lower(concat('%', :sku, '%')))
          and (:name is null or lower(tl.name) like lower(concat('%', :name, '%')))
          and (:txId is null or tl.transaction.id = :txId)
          and (:dateFrom is null or tl.transaction.createdAt >= :dateFrom)
          and (:dateTo is null or tl.transaction.createdAt <= :dateTo)
          and (:minQty is null or tl.qty >= :minQty)
          and (:maxQty is null or tl.qty <= :maxQty)
        order by tl.transaction.id desc, tl.id asc
    """)
    List<TxnLineDto> searchLinesForBusiness(
            @Param("businessId") Long businessId,
            @Param("sku") String sku,
            @Param("name") String name,
            @Param("txId") Long txId,
            @Param("dateFrom") java.time.OffsetDateTime dateFrom,
            @Param("dateTo") java.time.OffsetDateTime dateTo,
            @Param("minQty") Integer minQty,
            @Param("maxQty") Integer maxQty
    );

    @Query("""
      select coalesce(sum( (tl.unitPrice - p.buyPrice) * tl.qty ), 0)
      from TransactionLine tl
      join tl.transaction t
      join Product p on lower(p.sku) = lower(tl.sku) and p.businessId = t.businessId
      where t.businessId = :biz and t.createdAt between :start and :end
    """)
    BigDecimal sumProfit(Long biz, OffsetDateTime start, OffsetDateTime end);

    // sales by product
    @Query("""
      select new com.ipachi.pos.dto.reports.ProductSalesRow(
        tl.sku, tl.name, coalesce(sum(tl.lineTotal), 0)
      )
      from TransactionLine tl
      join tl.transaction t
      where t.businessId = :biz and t.createdAt between :start and :end
      group by tl.sku, tl.name
      order by coalesce(sum(tl.lineTotal), 0) desc
    """)
    List<ProductSalesRow> salesByProduct(Long biz, OffsetDateTime start, OffsetDateTime end);

    // profit by category (Σ profit)
    @Query("""
      select new com.ipachi.pos.dto.reports.CategoryProfitRow(
        coalesce(c.name, 'Uncategorized'),
        coalesce(sum( (tl.unitPrice - p.buyPrice) * tl.qty ), 0)
      )
      from TransactionLine tl
      join tl.transaction t
      join Product p on lower(p.sku) = lower(tl.sku) and p.businessId = t.businessId
      left join p.category c
      where t.businessId = :biz and t.createdAt between :start and :end
      group by c.name
      order by 2 desc
    """)
    List<CategoryProfitRow> profitByCategory(Long biz, OffsetDateTime start, OffsetDateTime end);
}
