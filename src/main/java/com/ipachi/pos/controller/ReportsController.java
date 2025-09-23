// src/main/java/com/ipachi/pos/web/ReportsController.java
package com.ipachi.pos.controller;

import com.ipachi.pos.dto.ApiResponse;
import com.ipachi.pos.dto.reports.*;
import com.ipachi.pos.service.ReportsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsService svc;

    private static OffsetDateTime defStart(OffsetDateTime start) {
        return start == null ? ReportsService.ytdStart() : start;
    }
    private static OffsetDateTime defEnd(OffsetDateTime end) {
        return end == null ? ReportsService.nowUtc() : end;
    }

    // ---------- Dashboard blocks ----------
    @GetMapping("/dashboard")
    public ApiResponse<DashboardKpis> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        return ApiResponse.ok(svc.dashboard(defStart(start), defEnd(end)));
    }

    @GetMapping("/sales-by-product")
    public ApiResponse<List<ProductSalesRow>> salesByProduct(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        return ApiResponse.ok(svc.salesByProduct(defStart(start), defEnd(end)));
    }

    @GetMapping("/monthly-trend")
    public ApiResponse<List<MonthlySalesRow>> monthlyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        return ApiResponse.ok(svc.monthlyTrend(defStart(start), defEnd(end)));
    }

    @GetMapping("/best-performers")
    public ApiResponse<List<ProductSalesRow>> bestPerformers(
            @RequestParam(defaultValue = "3") int top,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        return ApiResponse.ok(svc.bestPerformers(defStart(start), defEnd(end), Math.max(1, top)));
    }

    @GetMapping("/profit-by-category")
    public ApiResponse<List<CategoryProfitRow>> profitByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        return ApiResponse.ok(svc.profitByCategory(defStart(start), defEnd(end)));
    }

    @GetMapping("/sales-by-location")
    public ApiResponse<List<LocationSalesRow>> salesByLocation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        return ApiResponse.ok(svc.salesByLocation(defStart(start), defEnd(end)));
    }

    // ---------- Cash-Up ----------
    @GetMapping("/cashup")
    public ApiResponse<Map<String, Object>> cashup(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        var rows = svc.cashUpRows(start, end);
        var totals = svc.cashUpTotals(rows);
        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", rows);
        payload.put("totals", totals);
        return ApiResponse.ok(payload);
    }

    // ---------- Trade Account Statement ----------
    @GetMapping("/trade-account")
    public ApiResponse<TradeAccountStatement> tradeAccount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end
    ) {
        return ApiResponse.ok(svc.tradeAccount(start, end));
    }
}
