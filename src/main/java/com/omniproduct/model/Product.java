package com.omniproduct.model;

public class Product implements Identifiable {

    private ProductId id;
    private String name;
    private Price price;

    public Product(ProductId id, String name, Price price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public ProductId getId() { return id; }

    public String getName() { return name; }

    public Price getPrice() { return price; }

    public record Price(Amount base, Amount tax, TaxRate taxRate) {}

    public record Warehouse(Location location) {}

    public record Supplier(String name, Siren siren, TvaId tvaId) {}
}
