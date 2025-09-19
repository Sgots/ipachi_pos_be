package com.ipachi.pos.repo;

import com.ipachi.pos.model.ProductComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductComponentRepository extends JpaRepository<ProductComponent, Long> {

    @Query("SELECT pc FROM ProductComponent pc WHERE pc.parent.id = :parentId")
    List<ProductComponent> findByParentId(@Param("parentId") Long parentId);

    @Query("DELETE FROM ProductComponent pc WHERE pc.parent.id = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);

    @Query("SELECT pc FROM ProductComponent pc WHERE pc.parent.id = :parentId AND pc.userId = :userId")
    List<ProductComponent> findByParentIdAndUserId(@Param("parentId") Long parentId, @Param("userId") Long userId);

    @Query("DELETE FROM ProductComponent pc WHERE pc.parent.id = :parentId AND pc.userId = :userId")
    void deleteByParentIdAndUserId(@Param("parentId") Long parentId, @Param("userId") Long userId);
}