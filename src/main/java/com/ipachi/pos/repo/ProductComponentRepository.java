package com.ipachi.pos.repo;

import com.ipachi.pos.model.ProductComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductComponentRepository extends JpaRepository<ProductComponent, Long> {

    @Query("SELECT pc FROM ProductComponent pc WHERE pc.parent.id = :parentId")
    List<ProductComponent> findByParentId(@Param("parentId") Long parentId);

    // JPQL DELETE must be @Modifying and should return an int (rows affected)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM ProductComponent pc WHERE pc.parent.id = :parentId")
    int deleteByParentId(@Param("parentId") Long parentId);

    @Query("SELECT pc FROM ProductComponent pc WHERE pc.parent.id = :parentId AND pc.userId = :userId")
    List<ProductComponent> findByParentIdAndUserId(@Param("parentId") Long parentId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM ProductComponent pc WHERE pc.parent.id = :parentId AND pc.userId = :userId")
    int deleteByParentIdAndUserId(@Param("parentId") Long parentId, @Param("userId") Long userId);

    // OPTIONAL: Derived delete methods (no JPQL needed) if you prefer:
    // int deleteAllByParent_Id(Long parentId);
    // int deleteAllByParent_IdAndUserId(Long parentId, Long userId);
}
