// src/main/java/com/ipachi/pos/web/PromotionController.java
package com.ipachi.pos.controller;

import com.ipachi.pos.dto.ApiResponse;
import com.ipachi.pos.dto.PromoItemDto;
import com.ipachi.pos.dto.UpdatePromoRequest;
import com.ipachi.pos.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    /** GET /api/promotions/expired-shelflife?q=bread */
    @GetMapping("/expired-shelflife")
    public ApiResponse<List<PromoItemDto>> expiredShelfLife(
            @RequestParam(value = "q", required = false) String q) {
        return ApiResponse.ok(promotionService.expiredShelfLife(q));
    }

    /** PUT /api/promotions/{productId}  body: { newSellPrice?: number, onSpecial?: boolean } */
    @PutMapping("/{productId}")
    public ApiResponse<PromoItemDto> update(
            @PathVariable Long productId,
            @RequestBody UpdatePromoRequest req) {
        return ApiResponse.ok(promotionService.updateSellPriceAndLabel(productId, req));
    }
}
