// src/main/java/com/ipachi/pos/controller/StockReceiptController.java
package com.ipachi.pos.controller;

import com.ipachi.pos.dto.ReceiptFileView;
import com.ipachi.pos.dto.StockReceiptDto;
import com.ipachi.pos.service.StockReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/receipts")
@RequiredArgsConstructor
public class StockReceiptController {

    private final StockReceiptService receipts;

    @GetMapping
    public List<StockReceiptDto> search(@RequestParam(name = "q", required = false) String q) {
        return receipts.search(q);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable Long id) {
        ReceiptFileView v = receipts.getFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(v.contentType()))
                .header("Content-Disposition", "inline; filename=\"" + v.fileName() + "\"")
                .body(v.bytes()); // <-- was v.data()
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StockReceiptDto upload(
            @RequestParam("label") String label,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "receiptDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiptDate
    ) {
        return receipts.upload(label, file, receiptDate);
    }
}
