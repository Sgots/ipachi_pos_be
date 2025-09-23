package com.ipachi.pos.repo;

import com.ipachi.pos.dto.OutOfStockDto;
import com.ipachi.pos.dto.PromoRawRow;
import com.ipachi.pos.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("""
        select new com.ipachi.pos.dto.OutOfStockDto(
            p.sku,
            p.barcode,
            p.name,
            coalesce((select sum(m.quantityDelta)
                      from StockMovement m
                      where m.product = p and m.businessId = :bizId), 0),
            coalesce(u.name, 'unit')
        )
        from Product p
        left join p.unit u
        where p.businessId = :bizId
          and coalesce((select sum(m2.quantityDelta)
                        from StockMovement m2
                        where m2.product = p and m2.businessId = :bizId), 0) <= 0
          and (
               :q is null
            or lower(p.sku) like concat('%', lower(:q), '%')
            or lower(p.barcode) like concat('%', lower(:q), '%')
            or lower(p.name) like concat('%', lower(:q), '%')
          )
        order by p.name asc
    """)
    List<OutOfStockDto> findOutOfStockForBusiness(
            @Param("bizId") Long businessId,
            @Param("q") String query
    );
    Optional<Product> findByIdAndBusinessId(Long id, Long businessId);

    /* NEW: existence check for an id within a business */
    boolean existsByIdAndBusinessId(Long id, Long businessId);

    /* NEW: pageable listing by business */
    Page<Product> findByBusinessId(Long businessId, Pageable pageable);

    Optional<Product> findBySkuIgnoreCaseAndBusinessId(String sku, Long businessId);

    @Query("""
           SELECT p FROM Product p
           WHERE p.businessId = :businessId AND
                 (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.sku)  LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :q, '%')))
           """)
    Page<Product> searchByBusiness(@Param("businessId") Long businessId,
                                   @Param("q") String q,
                                   Pageable pageable);

    List<Product> findByBusinessIdOrderByNameAsc(Long businessId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE LOWER(p.sku) = LOWER(:sku) AND p.businessId = :businessId")
    boolean existsBySkuIgnoreCaseAndBusinessId(@Param("sku") String sku,
                                               @Param("businessId") Long businessId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE LOWER(p.barcode) = LOWER(:barcode) AND p.businessId = :businessId")
    boolean existsByBarcodeIgnoreCaseAndBusinessId(@Param("barcode") String barcode,
                                                   @Param("businessId") Long businessId);

    @Query("""
           SELECT COUNT(p) > 0
           FROM Product p
           WHERE LOWER(p.sku) = LOWER(:sku)
             AND p.businessId = :businessId
             AND p.id <> :id
           """)
    boolean existsBySkuIgnoreCaseAndBusinessIdAndIdNot(@Param("sku") String sku,
                                                       @Param("businessId") Long businessId,
                                                       @Param("id") Long id);

    @Query("""
           SELECT COUNT(p) > 0
           FROM Product p
           WHERE LOWER(p.barcode) = LOWER(:barcode)
             AND p.businessId = :businessId
             AND p.id <> :id
           """)
    boolean existsByBarcodeIgnoreCaseAndBusinessIdAndIdNot(@Param("barcode") String barcode,
                                                           @Param("businessId") Long businessId,
                                                           @Param("id") Long id);
    List<Product> findByBusinessId(Long businessId);
    @Query("""
        select new com.ipachi.pos.dto.PromoRawRow(
          p.id,
          p.sku,
          p.barcode,
          p.name,
          coalesce((select sum(m.quantityDelta)
                    from StockMovement m
                    where m.product = p and m.businessId = :bizId), 0),
          coalesce(u.name, 'unit'),
          p.lifetimeDays,
          (select max(m.createdAt)
           from StockMovement m
           where m.product = p and m.businessId = :bizId and m.quantityDelta > 0),
          p.buyPrice,
          p.sellPrice,
          p.onSpecial
        )
        from Product p
        left join p.unit u
        where p.businessId = :bizId
          and (:q is null or :q = '' or
               lower(p.sku) like concat('%', lower(:q), '%') or
               lower(p.barcode) like concat('%', lower(:q), '%') or
               lower(p.name) like concat('%', lower(:q), '%'))
        order by p.name asc
    """)
    List<PromoRawRow> promoCandidates(@Param("bizId") Long businessId,
                                      @Param("q") String query);

}
