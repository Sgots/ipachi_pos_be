package com.ipachi.pos.repo;

// src/main/java/com/ipachi/pos/repo/ProductComponentRepository.java

import com.ipachi.pos.model.ProductComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductComponentRepository extends JpaRepository<ProductComponent, Long> {
    List<ProductComponent> findByParentId(Long parentId);
    void deleteByParentId(Long parentId);
}
