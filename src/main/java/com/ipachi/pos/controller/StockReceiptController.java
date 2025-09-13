// src/main/java/com/ipachi/pos/controller/StockReceiptController.java
package com.ipachi.pos.controller;

import com.ipachi.pos.dto.StockReceiptDto;
import com.ipachi.pos.service.StockReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/receipts")
@RequiredArgsConstructor
public class StockReceiptController {
    private final StockReceiptService receipts;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StockReceiptDto upload(
            @RequestPart("label") String label,
            @RequestPart("file") MultipartFile file
    ) {
        return receipts.upload(label, file);
    }

    @GetMapping
    public List<StockReceiptDto> search(@RequestParam(name = "q", required = false) String q) {
        return receipts.search(q);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable Long id) {
        var file = receipts.getFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .cacheControl(CacheControl.noCache())
                .body(file.bytes());
    }
}
