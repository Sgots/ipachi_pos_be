// src/main/java/com/ipachi/pos/web/InventoryController.java
package com.ipachi.pos.controller;

import com.ipachi.pos.dto.ApiResponse;
import com.ipachi.pos.dto.OutOfStockDto;
import com.ipachi.pos.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final ProductService inventoryQueryService;

    /**
     * GET /api/inventory/out-of-stock?q=...
     * Returns products whose current stock (sum of movements) <= 0 for the current business.
     */
    @GetMapping("/out-of-stock")
    public ApiResponse<List<OutOfStockDto>> outOfStock(@RequestParam(value = "q", required = false) String q) {
        return ApiResponse.ok(inventoryQueryService.listOutOfStock(q));
    }
}
