package com.ipachi.pos.controller;

import com.ipachi.pos.model.InventoryItem;
import com.ipachi.pos.service.DataStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final DataStore store;
    public InventoryController(DataStore store) { this.store = store; }

    @GetMapping
    public ResponseEntity<List<InventoryItem>> list() {
        return ResponseEntity.ok(store.listInventory());
    }

    @PostMapping
    public ResponseEntity<InventoryItem> create(@Valid @RequestBody InventoryItem item) {
        return ResponseEntity.ok(store.addInventory(item));
    }

    @GetMapping("/lookup")
    public ResponseEntity<?> lookup(@RequestParam("sku") String sku) {
        return store.findBySku(sku)
            .<ResponseEntity<?>>map(i -> ResponseEntity.ok(Map.of("name", i.getName(), "price", i.getPrice())))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
