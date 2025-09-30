// src/main/java/com/ipachi/pos/service/ProductService.java
package com.ipachi.pos.service;

import com.ipachi.pos.dto.*;
import com.ipachi.pos.model.Category;
import com.ipachi.pos.model.MeasurementUnit;
import com.ipachi.pos.model.Product;
import com.ipachi.pos.model.ProductComponent;
import com.ipachi.pos.repo.*;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    @Autowired
    private QrService qrService;

    private final ProductRepository repo;
    private final CategoryRepository categories;
    private final MeasurementUnitRepository units;
    private final ProductComponentRepository componentRepo;
    private final StockMovementRepository stockMovementRepo;
    private final SettingsRepository settingsRepo; // VAT settings
    private final CurrentRequest ctx;

    private Long requireBusiness() {
        Long id = ctx.getBusinessId();
        if (id == null) throw new IllegalStateException("Business ID not found in request");
        return id;
    }
    private Long requireUser() {
        Long id = ctx.getUserId();
        if (id == null) throw new IllegalStateException("User ID not found in request");
        return id;
    }
    // in the same service that has create()/update()

    private void attachQrIfBarcodePresent(Product p) {
        String code = p.getBarcode();
        if (code == null || code.isBlank()) {
            p.setQrCodeData(null);
            p.setQrContentType(null);
            p.setQrFilename(null);
            return;
        }
        byte[] png = qrService.png(code, 512);
        p.setQrCodeData(png);
        p.setQrContentType("image/png");
        p.setQrFilename((p.getSku() == null ? "product" : p.getSku()) + "_qr.png");
    }

    // ----------------- utils -----------------
    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private Category resolveCategory(Long id) {
        if (id == null) return null;
        Long businessId = requireBusiness();
        return categories.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    }

    private MeasurementUnit resolveUnit(Long id) {
        if (id == null) return null;
        Long businessId = requireBusiness();
        return units.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit not found"));
    }

    private void attachImageIfPresent(Product p, MultipartFile image) {
        if (image == null || image.isEmpty()) return;
        try {
            p.setImageData(image.getBytes());
            p.setImageContentType(image.getContentType());
            p.setImageFilename(image.getOriginalFilename());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read image");
        }
    }

    private boolean hasComponents(Long parentId) {
        Long businessId = requireBusiness();
        return !componentRepo.findByParentIdAndBusinessId(parentId, businessId).isEmpty();
    }

    private BigDecimal totalItemCostsFromDb(Long parentId) {
        Long businessId = requireBusiness();
        return componentRepo.findByParentIdAndBusinessId(parentId, businessId).stream()
                .map(pc -> nz(pc.getUnitCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ----------------- CRUD -----------------
    public ProductDto create(ProductCreate req, MultipartFile image) {
        Long businessId = requireBusiness();
        Long userId = requireUser(); // who added

        final boolean isRecipe = req.productType() == ProductType.RECIPE
                || (req.components() != null && !req.components().isEmpty());

        final String rawSku = req.sku() == null ? "" : req.sku().trim();
        final String barcode = (req.barcode() == null || req.barcode().isBlank()) ? null : req.barcode().trim();

        if (rawSku.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU is required");
        if (repo.existsBySkuIgnoreCaseAndBusinessId(rawSku, businessId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists in this business");
        if (barcode != null && repo.existsByBarcodeIgnoreCaseAndBusinessId(barcode, businessId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Barcode already exists in this business");

        BigDecimal buy;
        if (isRecipe) {
            if (req.components() == null || req.components().isEmpty())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipe must have at least one ingredient");
            if (req.buyPrice() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buying price is required for recipes");
            buy = req.buyPrice().setScale(2, RoundingMode.HALF_UP);
        } else {
            if (req.buyPrice() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buying price is required");
            buy = req.buyPrice().setScale(2, RoundingMode.HALF_UP);
        }

        var p = Product.builder()
                .businessId(businessId)
                .createdByUserId(userId)
                .sku(rawSku)
                .barcode(barcode)
                .name(req.name().trim())
                .buyPrice(buy)
                // Store sellPrice as NET (VAT-exclusive)
                .sellPrice(nz(req.sellPrice()).setScale(2, RoundingMode.HALF_UP))
                .category(resolveCategory(req.categoryId()))
                .unit(resolveUnit(req.unitId()))
                .productType(isRecipe ? ProductType.RECIPE : ProductType.SINGLE)
                .saleMode(req.saleMode() == null ? ProductSaleMode.PER_UNIT : req.saleMode())
                .lifetimeDays(req.lifetime())
                .lowStock(req.lowStock())
                .userId(userId) // legacy/base owner
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .onSpecial(false)
                .build();

        attachImageIfPresent(p, image);
        // ⬇️ add this before first save OR right after save; either is fine
        attachQrIfBarcodePresent(p);
        p = repo.save(p);

        if (isRecipe) {
            replaceComponents(p, req.components()); // stamps biz & user
            p = repo.save(p);
        }

        return toDto(p);
    }

    // persists manual lines (component == null); measurement is free text
    private void replaceComponents(Product parent, List<ProductComponentCreate> lines) {
        Long businessId = requireBusiness();
        componentRepo.deleteByParentIdAndBusinessId(parent.getId(), businessId);
        if (lines == null || lines.isEmpty()) return;

        Long userId = requireUser();
        for (var l : lines) {
            var pc = ProductComponent.builder()
                    //.businessId(businessId)           // keep if schema has this column
                    .parent(parent)
                    .component(null)                  // manual
                    .name(l.name())
                    .measurementText(l.measurement())
                    .unitCost(l.unitCost() == null || l.unitCost().signum() < 0 ? BigDecimal.ZERO : l.unitCost())
                    .userId(userId)                   // who added the component
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            componentRepo.save(pc);
        }
    }

    public ProductDto update(Long id, ProductUpdate req, MultipartFile image) {
        Long businessId = requireBusiness();
        var p = repo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final String rawSku = req.sku() == null ? "" : req.sku().trim();
        final String barcode = (req.barcode() == null || req.barcode().isBlank()) ? null : req.barcode().trim();

        if (!rawSku.isBlank() && repo.existsBySkuIgnoreCaseAndBusinessIdAndIdNot(rawSku, businessId, id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists in this business");
        if (barcode != null && repo.existsByBarcodeIgnoreCaseAndBusinessIdAndIdNot(barcode, businessId, id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Barcode already exists in this business");
        if (rawSku.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU is required");

        p.setSku(rawSku);
        p.setBarcode(barcode);
        p.setName(req.name().trim());
        // Keep sellPrice NET (VAT-exclusive)
        p.setSellPrice(nz(req.sellPrice()).setScale(2, RoundingMode.HALF_UP));
        p.setCategory(resolveCategory(req.categoryId()));
        p.setUnit(resolveUnit(req.unitId()));
        if (req.saleMode() != null) p.setSaleMode(req.saleMode());
        p.setLifetimeDays(req.lifetime());
        p.setLowStock(req.lowStock());

        Boolean dtoIsRecipe = (req.productType() == null) ? null : (req.productType() == ProductType.RECIPE);
        boolean isRecipe = (dtoIsRecipe != null) ? dtoIsRecipe
                : (req.components() != null ? !req.components().isEmpty() : hasComponents(p.getId()));

        if (isRecipe) {
            if (req.components() != null) {
                if (req.components().isEmpty())
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipe must have at least one ingredient");
                replaceComponents(p, req.components());
            }
            if (req.buyPrice() != null) {
                p.setBuyPrice(req.buyPrice().setScale(2, RoundingMode.HALF_UP));
            }
            p.setProductType(ProductType.RECIPE);
        } else {
            if (req.components() != null && !req.components().isEmpty()) {
                componentRepo.deleteByParentIdAndBusinessId(p.getId(), businessId);
            }
            if (req.buyPrice() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buying price is required");
            p.setBuyPrice(req.buyPrice().setScale(2, RoundingMode.HALF_UP));
            p.setProductType(ProductType.SINGLE);
        }

        if (image != null && !image.isEmpty()) attachImageIfPresent(p, image);
        // ⬇️ add this before first save OR right after save; either is fine
        attachQrIfBarcodePresent(p);
        p.setUpdatedAt(OffsetDateTime.now());
        p = repo.save(p);
        return toDto(p);
    }

    public void delete(Long id) {
        Long businessId = requireBusiness();
        if (!repo.existsByIdAndBusinessId(id, businessId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        componentRepo.deleteByParentIdAndBusinessId(id, businessId);
        repo.deleteById(id);
    }

    public Page<ProductDto> list(String q, Pageable pageable, String baseImageUrl) {
        Long businessId = requireBusiness();
        var st = settingsRepo.findByBusinessId(businessId).orElse(null);

        // Settings fields from your Settings entity
        boolean enableVat  = st != null && st.isEnableVat();
        BigDecimal ratePct = (st != null && st.getVatRate() != null) ? st.getVatRate() : BigDecimal.ZERO;
        BigDecimal rate    = ratePct.movePointLeft(2);

        Page<Product> page = (q == null || q.isBlank())
                ? repo.findByBusinessId(businessId, pageable)
                : repo.searchByBusiness(businessId, q.trim(), pageable);

        var products = page.getContent();
        var ids = products.stream().map(Product::getId).toList();
        var availMap = computeAvailableMapForIds(ids, businessId);

        return page.map(p -> toDto(p, baseImageUrl, availMap.get(p.getId()),
                enableVat, /*pricesIncludeVat=*/false, ratePct, rate));
    }

    // Convenience for controller /all endpoint
    public List<ProductDto> all(String baseImageUrl) {
        Long businessId = requireBusiness();
        var st = settingsRepo.findByBusinessId(businessId).orElse(null);

        boolean enableVat  = st != null && st.isEnableVat();
        BigDecimal ratePct = (st != null && st.getVatRate() != null) ? st.getVatRate() : BigDecimal.ZERO;
        BigDecimal rate    = ratePct.movePointLeft(2);

        var list = repo.findByBusinessIdOrderByNameAsc(businessId);
        var ids = list.stream().map(Product::getId).toList();
        var availMap = computeAvailableMapForIds(ids, businessId);

        return list.stream()
                .map(p -> toDto(p, baseImageUrl, availMap.get(p.getId()),
                        enableVat, /*pricesIncludeVat=*/false, ratePct, rate))
                .toList();
    }

    public byte[] imageBytes(Long id, Long businessId) {
        var p = repo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (p.getImageData() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return p.getImageData();
    }

    public String imageContentType(Long id, Long businessId) {
        var p = repo.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return p.getImageContentType() == null ? "application/octet-stream" : p.getImageContentType();
    }

    // --- stock helpers ---
    private Map<Long, Integer> computeAvailableMapForIds(List<Long> ids, Long businessId) {
        var map = new HashMap<Long, Integer>();
        if (ids == null || ids.isEmpty()) return map;

        var rows = stockMovementRepo.totalsByProductIdAndBusinessId(businessId);
        for (Object[] row : rows) {
            if (row == null || row.length < 2) continue;
            Number pid = (Number) row[0];
            java.math.BigDecimal qty = (java.math.BigDecimal) row[1];
            if (pid == null) continue;
            int v = qty == null ? 0 : qty.setScale(0, RoundingMode.DOWN).intValue();
            map.put(pid.longValue(), v);
        }
        for (Long id : ids) map.putIfAbsent(id, 0);
        return map;
    }

    // ----------------- DTO mapping -----------------
    private ProductDto toDto(Product p) { return toDto(p, null, null); }
    private ProductDto toDto(Product p, String baseImageUrl) { return toDto(p, baseImageUrl, null); }

    // Default: loads settings (used by create/update single-return paths)
    private ProductDto toDto(Product p, String baseImageUrl, Integer availableQuantity) {
        Long businessId = requireBusiness();
        var st = settingsRepo.findByBusinessId(businessId).orElse(null);

        boolean enableVat  = st != null && st.isEnableVat();
        BigDecimal ratePct = (st != null && st.getVatRate() != null) ? st.getVatRate() : BigDecimal.ZERO;
        BigDecimal rate    = ratePct.movePointLeft(2);

        return toDto(p, baseImageUrl, availableQuantity, enableVat, /*pricesIncludeVat=*/false, ratePct, rate);
    }

    // VAT-aware overload used by list()/all() to avoid repeated settings lookups
    private ProductDto toDto(
            Product p,
            String baseImageUrl,
            Integer availableQuantity,
            boolean enableVat,
            boolean pricesIncludeVat, // ignored; stored price is always net
            BigDecimal ratePct,
            BigDecimal rate
    ) {
        boolean hasImage = p.getImageData() != null && p.getImageData().length > 0;
        String imageUrl = hasImage && baseImageUrl != null
                ? baseImageUrl + "/api/inventory/products/" + p.getId() + "/image"
                : null;

        var recipeCost = (p.getProductType() == ProductType.RECIPE)
                ? totalItemCostsFromDb(p.getId()) : null;

        // Always treat DB sellPrice as VAT-EXCLUSIVE (net)
        BigDecimal net  = nz(p.getSellPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal incl = (!enableVat || rate.signum() <= 0)
                ? net
                : net.multiply(BigDecimal.ONE.add(rate)).setScale(2, RoundingMode.HALF_UP);

        return new ProductDto(
                p.getId(), p.getSku(), p.getBarcode(), p.getName(),
                p.getBuyPrice(), p.getSellPrice(),
                p.getCategory() == null ? null : p.getCategory().getId(),
                p.getCategory() == null ? null : p.getCategory().getName(),
                p.getUnit() == null ? null : p.getUnit().getId(),
                p.getUnit() == null ? null : p.getUnit().getName(),
                p.getUnit() == null ? null : p.getUnit().getAbbr(),
                hasImage, imageUrl,
                p.getCreatedAt(), p.getUpdatedAt(),
                p.getProductType(),
                recipeCost,
                p.getLifetimeDays(),
                p.getLowStock(),
                p.getSaleMode(),
                availableQuantity,
                // match your response keys:
                incl,                 // priceInclVat
                net,                  // priceExclVat
                ratePct == null ? BigDecimal.ZERO : ratePct // vatRateApplied
        );
    }

    public List<ProductComponentDto> componentsOf(Long parentId) {
        Long businessId = requireBusiness();
        repo.findByIdAndBusinessId(parentId, businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return componentRepo.findByParentIdAndBusinessId(parentId, businessId).stream().map(pc -> {
            var u = pc.getUnit();
            var unitCost = nz(pc.getUnitCost());
            return new ProductComponentDto(
                    pc.getId(),
                    pc.getComponent() == null ? null : pc.getComponent().getId(),
                    pc.getComponent() == null ? null : pc.getComponent().getName(),
                    pc.getComponent() == null ? null : pc.getComponent().getSku(),
                    u == null ? null : u.getId(),
                    u == null ? null : u.getName(),
                    u == null ? null : u.getAbbr(),
                    pc.getMeasurementText(),
                    unitCost,
                    unitCost,
                    pc.getName()
            );
        }).toList();
    }

    public List<OutOfStockDto> listOutOfStock(String q) {
        Long bizId = ctx.getBusinessId();
        if (bizId == null) throw new IllegalStateException("X-Business-Id missing");
        return repo.findOutOfStockForBusiness(bizId, (q == null || q.isBlank()) ? null : q.trim());
    }
}
