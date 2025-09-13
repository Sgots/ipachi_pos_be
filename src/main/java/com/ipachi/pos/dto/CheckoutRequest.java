package com.ipachi.pos.dto;

import java.util.List;

public class CheckoutRequest {
    private Long customerId; // optional
    private String customerName; // optional
    private List<TillItem> items;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public List<TillItem> getItems() { return items; }
    public void setItems(List<TillItem> items) { this.items = items; }
}
