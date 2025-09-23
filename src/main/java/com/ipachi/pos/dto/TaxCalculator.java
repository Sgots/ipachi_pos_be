// src/main/java/com/ipachi/pos/tax/TaxCalculator.java
package com.ipachi.pos.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TaxCalculator {
    private TaxCalculator() {}

    public record Breakdown(BigDecimal net, BigDecimal vat, BigDecimal gross) {}

    public static Breakdown line(
            BigDecimal unitPrice, int qty,
            boolean pricesIncludeVat,
            BigDecimal ratePercent
    ) {
        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        BigDecimal q = BigDecimal.valueOf(Math.max(0, qty));
        BigDecimal base = price.multiply(q);

        BigDecimal rate = (ratePercent == null ? BigDecimal.ZERO : ratePercent).movePointLeft(2);
        if (rate.signum() <= 0) {
            // VAT disabled or zero
            BigDecimal n = base.setScale(2, RoundingMode.HALF_UP);
            return new Breakdown(n, BigDecimal.ZERO.setScale(2), n);
        }

        if (pricesIncludeVat) {
            BigDecimal divisor = BigDecimal.ONE.add(rate);
            BigDecimal net  = base.divide(divisor, 2, RoundingMode.HALF_UP);
            BigDecimal vat  = base.subtract(net).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gross = net.add(vat);
            return new Breakdown(net, vat, gross);
        } else {
            BigDecimal net  = base.setScale(2, RoundingMode.HALF_UP);
            BigDecimal vat  = net.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gross = net.add(vat);
            return new Breakdown(net, vat, gross);
        }
    }
}
