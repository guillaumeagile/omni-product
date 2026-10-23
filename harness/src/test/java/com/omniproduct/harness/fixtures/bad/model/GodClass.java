package com.omniproduct.harness.fixtures.bad.model;

// Volontairement mauvais : 6 propriétés publiques (HA5).
public class GodClass {
    private String name;
    private String slug;
    private int stock;
    private double price;
    private String supplier;
    private String region;

    public String getName() { return name; }
    public String getSlug() { return slug; }
    public int getStock() { return stock; }
    public double getPrice() { return price; }
    public String getSupplier() { return supplier; }
    public String getRegion() { return region; }
}
