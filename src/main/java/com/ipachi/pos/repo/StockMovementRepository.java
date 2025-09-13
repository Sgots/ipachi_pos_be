// src/main/java/com/ipachi/pos/repo/StockMovementRepository.java
package com.ipachi.pos.repo;

import com.ipachi.pos.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("select coalesce(sum(m.quantityDelta),0) " +
            "from StockMovement m where m.product.id = :productId")
    BigDecimal sumByProductId(@Param("productId") Long productId);

    @Query("select m.product.id, coalesce(sum(m.quantityDelta),0) " +
            "from StockMovement m group by m.product.id")
    List<Object[]> totalsByProduct();
}
