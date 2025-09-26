package com.ipachi.pos.repo;

import com.ipachi.pos.dto.TxnLineDto;
import com.ipachi.pos.dto.reports.CategoryProfitRow;
import com.ipachi.pos.dto.reports.ProductQtyProfitRow;
import com.ipachi.pos.dto.reports.ProductQtyProfitView;
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
            t.id,
            t.createdAt,
            tl.sku,
            tl.name,
            tl.qty,
            tl.grossAmount,
            tl.vatAmount,
            tl.profit,
            tl.createdByUserId,
            u.terminalId
        )
        from TransactionLine tl
        join tl.transaction t
        , User u
        where tl.businessId = :businessId
          and u.id = tl.createdByUserId
        order by t.id desc, tl.id asc
    """)
    List<TxnLineDto> findLinesForBusiness(@Param("businessId") Long businessId);

    @Query("""
        select new com.ipachi.pos.dto.TxnLineDto(
            t.id,
            t.createdAt,
            tl.sku,
            tl.name,
            tl.qty,
            tl.grossAmount,
            tl.vatAmount,
            tl.profit,
            tl.createdByUserId,
            u.terminalId
        )
        from TransactionLine tl
        join tl.transaction t
        , User u
        where tl.businessId = :businessId
          and u.id = tl.createdByUserId
          and (:sku is null or lower(tl.sku) like lower(concat('%', :sku, '%')))
          and (:name is null or lower(tl.name) like lower(concat('%', :name, '%')))
          and (:txId is null or t.id = :txId)
          and (:dateFrom is null or t.createdAt >= :dateFrom)
          and (:dateTo is null or t.createdAt <= :dateTo)
          and (:minQty is null or tl.qty >= :minQty)
          and (:maxQty is null or tl.qty <= :maxQty)
        order by t.id desc, tl.id asc
    """)
    List<TxnLineDto> searchLinesForBusiness(
            @Param("businessId") Long businessId,
            @Param("sku") String sku,
            @Param("name") String name,
            @Param("txId") Long txId,
            @Param("dateFrom") OffsetDateTime dateFrom,
            @Param("dateTo") OffsetDateTime dateTo,
            @Param("minQty") Integer minQty,
            @Param("maxQty") Integer maxQty
    );

    /* ===== Existing reporting queries ===== */

    @Query("""
  select coalesce(sum(tl.profit), 0)
  from TransactionLine tl
  join tl.transaction t
  where t.businessId = :biz
    and t.createdAt between :start and :end
""")
    BigDecimal sumProfit(Long biz, OffsetDateTime start, OffsetDateTime end);


    @Query("""
      select new com.ipachi.pos.dto.reports.ProductSalesRow(
        tl.sku, tl.name, coalesce(sum(tl.grossAmount), 0)
      )
      from TransactionLine tl
      join tl.transaction t
      where t.businessId = :biz and t.createdAt between :start and :end
      group by tl.sku, tl.name
      order by coalesce(sum(tl.grossAmount), 0) desc
    """)
    List<ProductSalesRow> salesByProduct(Long biz, OffsetDateTime start, OffsetDateTime end);
    // src/main/java/com/ipachi/pos/repo/TransactionLineRepository.java
    // src/main/java/com/ipachi/pos/repo/TransactionLineRepository.java
    @Query("""
  select 
     tl.sku        as sku,
     tl.name       as name,
     /* make SUM type-stable */
     sum(coalesce(tl.qty, 0))                    as qty,
     sum(coalesce(tl.profit, 0))                 as profit
  from TransactionLine tl
  join tl.transaction t
  where t.businessId = :biz
    and t.createdAt between :start and :end
  group by tl.sku, tl.name
  order by qty desc
""")
    List<ProductQtyProfitView> qtyAndProfitByProduct(
            Long biz, OffsetDateTime start, OffsetDateTime end);

    @Query("""
  select new com.ipachi.pos.dto.reports.CategoryProfitRow(
    coalesce(c.name, 'Uncategorized'),
    coalesce(sum(tl.profit), 0)
  )
  from TransactionLine tl
  join tl.transaction t
  left join Product p
    on lower(p.sku) = lower(tl.sku)
   and p.businessId = t.businessId
  left join p.category c
  where t.businessId = :biz
    and t.createdAt between :start and :end
  group by c.name
  order by 2 desc
""")
    List<CategoryProfitRow> profitByCategory(Long biz, OffsetDateTime start, OffsetDateTime end);

}
