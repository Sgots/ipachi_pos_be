// src/main/java/com/ipachi/pos/repo/ProductComponentRepository.java
package com.ipachi.pos.repo;

import com.ipachi.pos.model.ProductComponent;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductComponentRepository extends JpaRepository<ProductComponent, Long> {

    @Query("SELECT pc FROM ProductComponent pc WHERE pc.parent.id = :parentId AND pc.parent.businessId = :businessId")
    List<ProductComponent> findByParentIdAndBusinessId(@Param("parentId") Long parentId, @Param("businessId") Long businessId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM ProductComponent pc WHERE pc.parent.id = :parentId AND pc.parent.businessId = :businessId")
    int deleteByParentIdAndBusinessId(@Param("parentId") Long parentId, @Param("businessId") Long businessId);
}
