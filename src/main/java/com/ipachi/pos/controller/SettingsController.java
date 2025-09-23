package com.ipachi.pos.controller;

import com.ipachi.pos.model.Settings;
import com.ipachi.pos.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService service;

    /* ===== DTOs (API layer) ===== */

    public record SettingsResponse(
            String currency,
            String abbreviation,
            boolean enableVat,
            boolean pricesIncludeVat,
            BigDecimal vatRate
    ) {
        public static SettingsResponse from(Settings s) {
            if (s == null) return null;
            return new SettingsResponse(
                    s.getCurrency(),
                    s.getAbbreviation(),
                    s.isEnableVat(),
                    s.isPricesIncludeVat(),
                    s.getVatRate()
            );
        }
    }

    public record SettingsCreateBody(
            String currency,
            String abbreviation,
            Boolean enableVat,
            Boolean pricesIncludeVat,
            BigDecimal vatRate
    ) {}

    public record SettingsUpsertBody(
            String currency,
            String abbreviation,
            Boolean enableVat,
            Boolean pricesIncludeVat,
            BigDecimal vatRate
    ) {}

    public record TaxSettingsBody(
            Boolean enableVat,
            Boolean pricesIncludeVat,
            BigDecimal vatRate
    ) {}

    /* ===== Endpoints ===== */

    /** Get full settings for the current business. */
    @GetMapping
    public SettingsResponse get() {
        return SettingsResponse.from(service.getCurrentSettings());
    }

    /** Create settings for the current business (fails if already exist). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SettingsResponse create(@RequestBody SettingsCreateBody body) {
        var created = service.create(new SettingsService.SettingsCreateRequest(
                body.currency(),
                body.abbreviation(),
                body.enableVat(),
                body.pricesIncludeVat(),
                body.vatRate()
        ));
        return SettingsResponse.from(created);
    }

    /** Upsert (create if missing) full settings for the current business. */
    @PutMapping
    public SettingsResponse upsert(@RequestBody SettingsUpsertBody body) {
        var saved = service.upsert(new SettingsService.SettingsUpsertRequest(
                body.currency(),
                body.abbreviation(),
                body.enableVat(),
                body.pricesIncludeVat(),
                body.vatRate()
        ));
        return SettingsResponse.from(saved);
    }

    /** Get tax-only settings (compact). */
    @GetMapping("/tax")
    public TaxSettingsBody getTax() {
        var s = service.getCurrentSettings();
        if (s == null) return new TaxSettingsBody(false, false, BigDecimal.ZERO);
        return new TaxSettingsBody(s.isEnableVat(), s.isPricesIncludeVat(), s.getVatRate());
    }

    /** Update tax-only settings (creates Settings if missing). */
    @PutMapping("/tax")
    public TaxSettingsBody updateTax(@RequestBody TaxSettingsBody body) {
        var saved = service.updateTax(new SettingsService.TaxSettingsRequest(
                body.enableVat(),
                body.pricesIncludeVat(),
                body.vatRate()
        ));
        return new TaxSettingsBody(saved.isEnableVat(), saved.isPricesIncludeVat(), saved.getVatRate());
    }
}
