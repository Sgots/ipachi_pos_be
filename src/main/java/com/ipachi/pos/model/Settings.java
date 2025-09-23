package com.ipachi.pos.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// src/main/java/com/ipachi/pos/model/Settings.java
@Entity
@Table(name = "settings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_settings_business", columnNames = {"business_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    private String currency;
    private String abbreviation;

    // NEW
    @Column(name = "enable_vat", nullable = false)
    private boolean enableVat = false;

    // If catalog prices already include VAT (true) vs exclude (false)
    @Column(name = "prices_include_vat", nullable = false)
    private boolean pricesIncludeVat = false;

    // Default VAT %, e.g. 14.00 for 14%
    @Column(name = "vat_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal vatRate = BigDecimal.ZERO;
}
