package com.ipachi.pos.model;

import java.time.OffsetDateTime;

public class Transaction {
    private Long id;
    private OffsetDateTime date;
    private String customer; // display name
    private double total;

    public Transaction() {}

    public Transaction(Long id, OffsetDateTime date, String customer, double total) {
        this.id = id;
        this.date = date;
        this.customer = customer;
        this.total = total;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OffsetDateTime getDate() { return date; }
    public void setDate(OffsetDateTime date) { this.date = date; }
    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}
