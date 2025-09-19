package com.ipachi.pos.repo;


import com.ipachi.pos.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySku(String sku);

    List<InventoryItem> findByUserId(Long userId);
    Optional<InventoryItem> findByIdAndUserId(Long id, Long userId);
}
