package com.omniproduct.model;

// Product ne garde que son identité. Chaque préoccupation (catalogue, prix, stock, transport, approvisionnement)
// est une valeur à part, reliée au produit par son identifiant.
public class Product implements Identifiable {

    private String id;
    private String name;

    public Product() {}

    public Product(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public record Price(Double base, Double tax, Double taxRate) {}

    public record Warehouse(String location) {}

    public record Supplier(String name, String siren, String tvaId) {}
}
