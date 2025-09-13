package com.ipachi.pos.dto;

public class TillItem {
    private String sku;
    private String name;
    private double price;
    private int qty;

    public TillItem() {}

    public TillItem(String sku, String name, double price, int qty) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.qty = qty;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
}
