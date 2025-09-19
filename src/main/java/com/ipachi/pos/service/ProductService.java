package com.ipachi.pos.service;

import com.ipachi.pos.dto.ProductComponentCreate;
import com.ipachi.pos.dto.ProductComponentDto;
import com.ipachi.pos.dto.ProductCreate;
import com.ipachi.pos.dto.ProductDto;
import com.ipachi.pos.dto.ProductSaleMode;
import com.ipachi.pos.dto.ProductType;
import com.ipachi.pos.dto.ProductUpdate;
import com.ipachi.pos.model.Category;
import com.ipachi.pos.model.MeasurementUnit;
import com.ipachi.pos.model.Product;
import com.ipachi.pos.model.ProductComponent;
import com.ipachi.pos.repo.CategoryRepository;
import com.ipachi.pos.repo.MeasurementUnitRepository;
import com.ipachi.pos.repo.ProductComponentRepository;
import com.ipachi.pos.repo.ProductRepository;
import com.ipachi.pos.repo.StockMovementRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
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

    private final ProductRepository repo;
    private final CategoryRepository categories;
    private final MeasurementUnitRepository units;
    private final ProductComponentRepository componentRepo;
    private final StockMovementRepository stockMovementRepo;
    private final CurrentRequest ctx;

    // Helper method to validate user context
    private Long validateUserContext() {
        Long userId = ctx.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request context");
        }
        return userId;
    }

    // ----------------- utils -----------------
    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private Category resolveCategory(Long id) {
        Long userId = validateUserContext();
        if (id == null) return null;
        return categories.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    }

    private MeasurementUnit resolveUnit(Long id) {
        Long userId = validateUserContext();
        if (id == null) return null;
        return units.findByIdAndUserId(id, userId)
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
        Long userId = validateUserContext();
        return !componentRepo.findByParentIdAndUserId(parentId, userId).isEmpty();
    }

    // cost = sum of item costs (measurement text is not multiplied)
    private BigDecimal totalItemCostsFromRequest(List<ProductComponentCreate> lines) {
        if (lines == null || lines.isEmpty()) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = BigDecimal.ZERO;
        for (var l : lines) {
            var cost = l.unitCost();
            if (cost == null || cost.signum() < 0) cost = BigDecimal.ZERO;
            total = total.add(cost);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal totalItemCostsFromDb(Long parentId) {
        Long userId = validateUserContext();
        return componentRepo.findByParentIdAndUserId(parentId, userId).stream()
                .map(pc -> nz(pc.getUnitCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // persists manual lines (component == null); measurement is free text
    private void replaceComponents(Product parent, List<ProductComponentCreate> lines) {
        Long userId = validateUserContext();
        componentRepo.deleteByParentIdAndUserId(parent.getId(), userId);
        if (lines == null || lines.isEmpty()) return;

        for (var l : lines) {
            var pc = ProductComponent.builder()
                    .parent(parent)
                    .component(null) // manual
                    .name(l.name())
                    .measurementText(l.measurement())
                    .unitCost(l.unitCost() == null || l.unitCost().signum() < 0 ? BigDecimal.ZERO : l.unitCost())
                    .userId(userId)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            componentRepo.save(pc);
        }
    }

    public List<ProductComponentDto> componentsOf(Long parentId) {
        Long userId = validateUserContext();
        repo.findByIdAndUserId(parentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return componentRepo.findByParentIdAndUserId(parentId, userId).stream().map(pc -> {
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

    // ----------------- CRUD -----------------
    public ProductDto create(ProductCreate req, MultipartFile image) {
        Long userId = validateUserContext();

        final boolean isRecipe = req.productType() == ProductType.RECIPE
                || (req.components() != null && !req.components().isEmpty());

        final String rawSku = req.sku() == null ? "" : req.sku().trim();
        final String barcode = (req.barcode() == null || req.barcode().isBlank()) ? null : req.barcode().trim();

        if (rawSku.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU is required");
        if (repo.existsBySkuIgnoreCaseAndUserId(rawSku, userId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists");
        if (barcode != null && repo.existsByBarcodeIgnoreCaseAndUserId(barcode, userId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Barcode already exists");

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
                .sku(rawSku)
                .barcode(barcode)
                .name(req.name().trim())
                .buyPrice(buy)
                .sellPrice(nz(req.sellPrice()).setScale(2, RoundingMode.HALF_UP))
                .category(resolveCategory(req.categoryId()))
                .unit(resolveUnit(req.unitId()))
                .productType(isRecipe ? ProductType.RECIPE : ProductType.SINGLE)
                .saleMode(req.saleMode() == null ? ProductSaleMode.PER_UNIT : req.saleMode())
                .lifetime(req.lifetime())
                .lowStock(req.lowStock())
                .userId(userId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        attachImageIfPresent(p, image);
        p = repo.save(p);

        if (isRecipe) {
            replaceComponents(p, req.components());
            p = repo.save(p);
        }

        return toDto(p);
    }

    public ProductDto update(Long id, ProductUpdate req, MultipartFile image) {
        Long userId = validateUserContext();

        var p = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final String rawSku = req.sku() == null ? "" : req.sku().trim();
        final String barcode = (req.barcode() == null || req.barcode().isBlank()) ? null : req.barcode().trim();

        if (!rawSku.isBlank() && repo.existsBySkuIgnoreCaseAndUserIdAndIdNot(rawSku, userId, id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists");
        if (barcode != null && repo.existsByBarcodeIgnoreCaseAndUserIdAndIdNot(barcode, userId, id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Barcode already exists");
        if (rawSku.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU is required");

        p.setSku(rawSku);
        p.setBarcode(barcode);
        p.setName(req.name().trim());
        p.setSellPrice(nz(req.sellPrice()).setScale(2, RoundingMode.HALF_UP));
        p.setCategory(resolveCategory(req.categoryId()));
        p.setUnit(resolveUnit(req.unitId()));
        if (req.saleMode() != null) p.setSaleMode(req.saleMode());
        p.setLifetime(req.lifetime());
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
                componentRepo.deleteByParentIdAndUserId(p.getId(), userId);
            }
            if (req.buyPrice() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buying price is required");
            p.setBuyPrice(req.buyPrice().setScale(2, RoundingMode.HALF_UP));
            p.setProductType(ProductType.SINGLE);
        }

        if (image != null && !image.isEmpty()) attachImageIfPresent(p, image);
        p.setUpdatedAt(OffsetDateTime.now());
        p = repo.save(p);
        return toDto(p);
    }

    public void delete(Long id) {
        Long userId = validateUserContext();

        if (!repo.existsByIdAndUserId(id, userId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        componentRepo.deleteByParentIdAndUserId(id, userId);
        repo.deleteById(id);
    }

    public Page<ProductDto> list(String q, Pageable pageable, String baseImageUrl) {
        Long userId = validateUserContext();

        var page = (q == null || q.isBlank())
                ? repo.findByUserId(userId, pageable)
                : repo.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrBarcodeContainingIgnoreCaseAndUserId(
                q.trim(), q.trim(), q.trim(), userId, pageable);

        var products = page.getContent();
        var ids = products.stream().map(Product::getId).toList();
        var availMap = computeAvailableMapForIds(ids, userId);

        return page.map(p -> toDto(p, baseImageUrl, availMap.get(p.getId())));
    }

    public List<ProductDto> all(String baseImageUrl) {
        Long userId = validateUserContext();

        var list = repo.findByUserIdOrderByNameAsc(userId);
        var ids = list.stream().map(Product::getId).toList();
        var availMap = computeAvailableMapForIds(ids, userId);
        return list.stream().map(p -> toDto(p, baseImageUrl, availMap.get(p.getId()))).toList();
    }

    public byte[] imageBytes(Long id, Long userId) {
        var p = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (p.getImageData() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return p.getImageData();
    }

    public String imageContentType(Long id, Long userId) {
        var p = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return p.getImageContentType() == null ? "application/octet-stream" : p.getImageContentType();
    }

    // --- stock helpers ---
    /**
     * Compute available quantity (Integer) for a set of product ids for the given user.
     * Returns a map productId -> availableQuantity (defaults to 0 for missing entries).
     */
    private Map<Long, Integer> computeAvailableMapForIds(List<Long> ids, Long userId) {
        var map = new HashMap<Long, Integer>();
        if (ids == null || ids.isEmpty()) return map;

        var rows = stockMovementRepo.totalsByProductIdAndUserId(userId);
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

    // ----------------- DTO mappers -----------------
    private ProductComponentDto toDto(ProductComponent pc) {
        var cost = nz(pc.getUnitCost());
        return new ProductComponentDto(
                pc.getId(),
                pc.getComponent() == null ? null : pc.getComponent().getId(),
                pc.getComponent() == null ? null : pc.getComponent().getName(),
                pc.getComponent() == null ? null : pc.getComponent().getSku(),
                null, null, null,
                pc.getMeasurementText(),
                cost,
                cost,
                pc.getName()
        );
    }

    private ProductDto toDto(Product p) { return toDto(p, null, null); }

    private ProductDto toDto(Product p, String baseImageUrl) { return toDto(p, baseImageUrl, null); }

    private ProductDto toDto(Product p, String baseImageUrl, Integer availableQuantity) {
        boolean hasImage = p.getImageData() != null && p.getImageData().length > 0;
        String imageUrl = hasImage && baseImageUrl != null
                ? baseImageUrl + "/api/inventory/products/" + p.getId() + "/image"
                : null;

        var recipeCost = (p.getProductType() == ProductType.RECIPE)
                ? totalItemCostsFromDb(p.getId()) : null;

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
                p.getLifetime(),
                p.getLowStock(),
                p.getSaleMode(),
                availableQuantity
        );
    }
}
