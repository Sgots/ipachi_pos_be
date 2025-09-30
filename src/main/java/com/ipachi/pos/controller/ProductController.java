// src/main/java/com/ipachi/pos/controller/ProductController.java
package com.ipachi.pos.controller;

import com.ipachi.pos.dto.*;
import com.ipachi.pos.model.Product;
import com.ipachi.pos.repo.ProductRepository;
import com.ipachi.pos.service.ProductService;
import com.ipachi.pos.service.StockService;
import com.ipachi.pos.security.CurrentRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Enumeration;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;
    private final StockService stockService;
    private final CurrentRequest ctx;
    private final ProductRepository productRepository;

    private String base(HttpServletRequest req) {
        var url = req.getRequestURL().toString();
        return url.substring(0, url.indexOf(req.getRequestURI()));
    }

    // Create (JSON)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createJson(@RequestBody @Valid ProductCreate req, HttpServletRequest http) {
        return service.create(req, null);
    }

    // Create (multipart: data + image)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createMultipart(
            @RequestPart(name = "data") @Valid ProductCreate req,
            @RequestPart(name = "image", required = false) MultipartFile image
    ) {
        return service.create(req, image);
    }

    // Update (JSON)
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductDto updateJson(
            @PathVariable(name = "id") Long id,
            @RequestBody @Valid ProductUpdate req
    ) {
        return service.update(id, req, null);
    }

    // Update (multipart)
    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductDto updateMultipart(
            @PathVariable(name = "id") Long id,
            @RequestPart(name = "data") @Valid ProductUpdate req,
            @RequestPart(name = "image", required = false) MultipartFile image
    ) {
        return service.update(id, req, image);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable(name = "id") Long id, HttpServletRequest http) {
        // DEBUG remains (optional)
        System.out.println("=== IMAGE REQUEST DEBUG ===");
        System.out.println("Product ID: " + id);
        Enumeration<String> headerNames = http.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println("  " + headerName + ": " + http.getHeader(headerName));
        }

        Long businessId = ctx.getBusinessId();
        if (businessId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            var bytes = service.imageBytes(id, businessId);
            var type  = service.imageContentType(id, businessId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(type))
                    .cacheControl(CacheControl.noCache())
                    .body(bytes);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return ResponseEntity.notFound().build();
            }
            throw ex;
        }
    }
    // src/main/java/com/ipachi/pos/web/InventoryProductController.java
    @GetMapping(value = "/{id}/qr.png")
    public ResponseEntity<byte[]> downloadQr(@PathVariable Long id) {
        Product p = productRepository.findByIdAndBusinessId(id, ctx.getBusinessId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        byte[] data = p.getQrCodeData();
        if (data == null || data.length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No QR code for this product");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, p.getQrContentType() == null ? "image/png" : p.getQrContentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        (p.getQrFilename() == null ? ("product-" + p.getId() + ".png") : p.getQrFilename()) + "\"")
                .body(data);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "id") Long id) {
        service.delete(id);
    }

    @GetMapping
    public Page<ProductDto> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "name,asc") String sort,
            HttpServletRequest http
    ) {
        Sort sortObj = Sort.by(sort.split(",")[0]).ascending();
        if (sort.toLowerCase().endsWith(",desc")) sortObj = sortObj.descending();
        return service.list(q, PageRequest.of(page, size, sortObj), base(http));
    }

    @GetMapping("/all")
    public List<ProductDto> all(HttpServletRequest http) {
        return service.all(base(http));
    }

    @GetMapping("/{id}/components")
    public List<ProductComponentDto> components(@PathVariable("id") Long id) {
        return service.componentsOf(id);
    }

    @PostMapping(path = "/{id}/restock", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestockResponse restock(@PathVariable("id") Long id, @RequestBody RestockRequest req) {
        return stockService.restock(id, req);
    }

}
