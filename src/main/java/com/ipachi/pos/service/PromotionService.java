// src/main/java/com/ipachi/pos/service/PromotionService.java
package com.ipachi.pos.service;

import com.ipachi.pos.dto.PromoItemDto;
import com.ipachi.pos.dto.PromoRawRow;
import com.ipachi.pos.dto.UpdatePromoRequest;
import com.ipachi.pos.model.Product;
import com.ipachi.pos.repo.ProductRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final ProductRepository productRepository;
    private final CurrentRequest ctx;

    public List<PromoItemDto> expiredShelfLife(String q) {
        Long bizId = requireBiz();
        List<PromoRawRow> raw = productRepository.promoCandidates(bizId, q);

        OffsetDateTime now = OffsetDateTime.now();

        return raw.stream()
                .filter(r -> r.getLifetimeDays() != null && r.getLifetimeDays() > 0)
                .filter(r -> r.getLastRestockedAt() != null)        // need a restock time to compare
                .map(r -> {
                    int days = (int) ChronoUnit.DAYS.between(r.getLastRestockedAt().toLocalDate(), now.toLocalDate());
                    return new PromoItemDto(
                            r.getProductId(),
                            r.getSku(),
                            r.getBarcode(),
                            r.getName(),
                            r.getCurrentStock() == null ? BigDecimal.ZERO : r.getCurrentStock(),
                            r.getUnitName(),
                            r.getLifetimeDays(),
                            days,
                            r.getBuyPrice(),
                            r.getSellPrice(),
                            r.getOnSpecial() != null && r.getOnSpecial()
                    );
                })
                .filter(d -> d.getInStockForDays() > d.getLifetimeDays()) // “passed shelf life”
                // You can decide if you want to also require quantity > 0:
                // .filter(d -> d.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(PromoItemDto::getInStockForDays).reversed())
                .toList();
    }

    @Transactional
    public PromoItemDto updateSellPriceAndLabel(Long productId, UpdatePromoRequest req) {
        Long bizId = requireBiz();
        Product p = productRepository.findByIdAndBusinessId(productId, bizId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or not in your business"));

        if (req.getNewSellPrice() != null) {
            p.setSellPrice(req.getNewSellPrice());
        }
        if (req.getOnSpecial() != null) {
            p.setOnSpecial(req.getOnSpecial());
        }
        productRepository.save(p);

        // rehydrate a minimal response (inStockForDays recompute not necessary here)
        return new PromoItemDto(
                p.getId(),
                p.getSku(),
                p.getBarcode(),
                p.getName(),
                null, // unchanged here
                p.getUnit() != null ? p.getUnit().getName() : "unit",
                p.getLifetimeDays(),
                null,
                p.getBuyPrice(),
                p.getSellPrice(),
                p.getOnSpecial() != null && p.getOnSpecial()
        );
    }

    private Long requireBiz() {
        Long v = ctx.getBusinessId();
        if (v == null) throw new IllegalStateException("X-Business-Id missing");
        return v;
    }
}
