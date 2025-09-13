package com.ipachi.pos.controller;

// src/main/java/com/ipachi/pos/controller/StockController.java

import com.ipachi.pos.dto.StockItemDto;
import com.ipachi.pos.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/stock")
@RequiredArgsConstructor
public class StockController {
    private final StockService stock;

    @GetMapping
    public List<StockItemDto> list(@RequestParam(name = "q", required = false) String q) {
        return stock.list(q);
    }
}
