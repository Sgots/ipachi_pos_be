package com.ipachi.pos.controller;

import com.ipachi.pos.dto.ApiResponse;
import com.ipachi.pos.dto.TxnLineDto;
import com.ipachi.pos.service.TransactionQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;



@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionQueryService service;

    public TransactionController(TransactionQueryService service) {
        this.service = service;
    }
    @GetMapping("/lines")
    public ResponseEntity<ApiResponse<List<TxnLineDto>>> searchLines(
            @RequestParam(value = "businessId", required = false) Long businessIdParam,
            @RequestHeader(value = "X-Business-Id", required = false) Long businessIdHeader,
            @RequestParam(value = "sku", required = false) String sku,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "txId", required = false) Long txId,
            @RequestParam(value = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(value = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(value = "minQty", required = false) Integer minQty,
            @RequestParam(value = "maxQty", required = false) Integer maxQty
    ) {
        Long businessId = businessIdParam != null ? businessIdParam : businessIdHeader;
        if (businessId == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(0,
                    "Missing businessId (query param or X-Business-Id header)", List.of()));
        }


        var rows = service.searchLinesByBusiness(businessId, sku, name, txId, dateFrom, dateTo, minQty, maxQty);
        return ResponseEntity.ok(new ApiResponse<>(1, "Success", rows));
    }

}
