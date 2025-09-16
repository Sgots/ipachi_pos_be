package com.ipachi.pos.dto;


        import java.math.BigDecimal;

        public class TillSummary {
    public BigDecimal openingFloat;
    public BigDecimal sales;
    public BigDecimal refunds;
    public BigDecimal cashIn;
    public BigDecimal cashOut;
    public BigDecimal payouts;
    public BigDecimal expectedCash;
    public BigDecimal closingCashActual;
    public BigDecimal overShort;
}
