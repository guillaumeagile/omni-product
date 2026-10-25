package com.omniproduct.model;

// Product garde son identité, son nom et son prix. Les autres préoccupations (catalogue, stock, transport,
// approvisionnement) sont des valeurs à part, reliées au produit par son identifiant.
public class Product implements Identifiable {

    private String id;
    private String name;
    private Price price;

    public Product(String id, String name, Price price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    @Override
    public String getId() { return id; }

    public String getName() { return name; }

    public Price getPrice() { return price; }

    public record Price(Double base, Double tax, Double taxRate) {}

    public record Warehouse(String location) {}

    public record Supplier(String name, String siren, String tvaId) {}
}
