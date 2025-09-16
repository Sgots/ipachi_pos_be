package com.ipachi.pos.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Simple DTO returned after a successful checkout.
 * Matches the controller usage: transaction id, timestamp, total, customer name, and a message.
 */
public class CheckoutResponse {

    private Long transactionId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime createdAt;

    private BigDecimal total;
    private String customerName;
    private String message;

    public CheckoutResponse() { }

    public CheckoutResponse(Long transactionId,
                            OffsetDateTime createdAt,
                            BigDecimal total,
                            String customerName,
                            String message) {
        this.transactionId = transactionId;
        this.createdAt = createdAt;
        this.total = total;
        this.customerName = customerName;
        this.message = message;
    }

    // Getters & Setters
    public Long getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getTotal() {
        return total;
    }
    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getCustomerName() {
        return customerName;
    }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
