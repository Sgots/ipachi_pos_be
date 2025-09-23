package com.ipachi.pos.service;

import com.ipachi.pos.model.Settings;
import com.ipachi.pos.repo.SettingsRepository;
import com.ipachi.pos.security.CurrentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final CurrentRequest currentRequest;

    private Long requireBusinessId() {
        Long id = currentRequest.getBusinessId();
        if (id == null) throw new IllegalStateException("X-Business-Id missing");
        return id;
    }

    /* ========= Queries ========= */

    /** Returns settings for the current business, or null if none exist yet. */
    @Transactional(readOnly = true)
    public Settings getCurrentSettings() {
        Long businessId = requireBusinessId();
        return settingsRepository.findByBusinessId(businessId).orElse(null);
    }

    /** Returns settings for the given business, or empty if none exist. */
    @Transactional(readOnly = true)
    public Optional<Settings> findByBusinessId(Long businessId) {
        return settingsRepository.findByBusinessId(businessId);
    }

    /* ========= Create / Update ========= */

    /** Creates settings for the current business. Fails if they already exist. */
    public Settings create(SettingsCreateRequest req) {
        Long businessId = requireBusinessId();
        if (settingsRepository.findByBusinessId(businessId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settings already exist for this business");
        }

        Settings s = new Settings();
        s.setBusinessId(businessId);
        s.setCurrency(trimOrNull(req.currency()));
        s.setAbbreviation(trimOrNull(req.abbreviation()));
        s.setEnableVat(Boolean.TRUE.equals(req.enableVat()));
        s.setPricesIncludeVat(Boolean.TRUE.equals(req.pricesIncludeVat()));
        s.setVatRate(nz(req.vatRate()));

        try {
            return settingsRepository.save(s);
        } catch (DataIntegrityViolationException e) {
            // In case of a race creating the single row per business
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settings already exist for this business");
        }
    }

    /** Upserts (create if missing, else update) currency/abbr and tax fields for the current business. */
    public Settings upsert(SettingsUpsertRequest req) {
        Long businessId = requireBusinessId();
        Settings s = settingsRepository.findByBusinessId(businessId).orElseGet(() -> {
            Settings n = new Settings();
            n.setBusinessId(businessId);
            return n;
        });

        if (req.currency() != null)      s.setCurrency(trimOrNull(req.currency()));
        if (req.abbreviation() != null)  s.setAbbreviation(trimOrNull(req.abbreviation()));
        if (req.enableVat() != null)     s.setEnableVat(req.enableVat());
        if (req.pricesIncludeVat() != null) s.setPricesIncludeVat(req.pricesIncludeVat());
        if (req.vatRate() != null)       s.setVatRate(nz(req.vatRate()));

        return settingsRepository.save(s);
    }

    /** Updates only the tax fields (enableVat, pricesIncludeVat, vatRate). Creates settings if missing. */
    public Settings updateTax(TaxSettingsRequest req) {
        Long businessId = requireBusinessId();
        Settings s = settingsRepository.findByBusinessId(businessId).orElseGet(() -> {
            Settings n = new Settings();
            n.setBusinessId(businessId);
            return n;
        });

        if (req.enableVat() != null)        s.setEnableVat(req.enableVat());
        if (req.pricesIncludeVat() != null) s.setPricesIncludeVat(req.pricesIncludeVat());
        if (req.vatRate() != null)          s.setVatRate(nz(req.vatRate()));

        return settingsRepository.save(s);
    }

    /* ========= DTOs (service-level) ========= */

    public record SettingsCreateRequest(
            String currency,
            String abbreviation,
            Boolean enableVat,
            Boolean pricesIncludeVat,
            BigDecimal vatRate
    ) {}

    public record SettingsUpsertRequest(
            String currency,
            String abbreviation,
            Boolean enableVat,
            Boolean pricesIncludeVat,
            BigDecimal vatRate
    ) {}

    public record TaxSettingsRequest(
            Boolean enableVat,
            Boolean pricesIncludeVat,
            BigDecimal vatRate
    ) {}

    /* ========= Helpers ========= */

    private static String trimOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
