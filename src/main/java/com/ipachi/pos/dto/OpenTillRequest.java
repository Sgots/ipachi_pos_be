package com.ipachi.pos.dto;


        import java.math.BigDecimal;

        public class OpenTillRequest {
    public String terminalId;
    public Long openedByUserId;
    public BigDecimal openingFloat;
    public String notes;
}
