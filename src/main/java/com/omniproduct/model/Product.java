package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;

    @Embedded
    private Price price;

    @Embedded
    private Catalog catalog;

    @Embedded
    private Storage storage;

    @Embedded
    private Supply supply;

    // Default constructor for JPA
    public Product() {
        this.catalog = new Catalog();
        this.storage = new Storage();
        this.supply = new Supply();
    }

    // Constructor for testing
    public Product(String id, String name, String slug, Price price, List<String> discounts,
                   Map<String, String> images, Map<String, SupplierIdentity> suppliersRegions, Double weight, String dimensions,
                   Integer quantity, Integer stock, Warehouse warehouse) {
        this.id = id;
        this.price = price;
        this.catalog = new Catalog(name, slug, discounts, images);
        this.storage = new Storage(quantity, stock, warehouse);
        this.supply = new Supply(null, suppliersRegions, weight, dimensions);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Price getPrice() { return price; }
    public void setPrice(Price price) { this.price = price; }

    @JsonUnwrapped
    public Catalog getCatalog() { return catalog; }
    public void setCatalog(Catalog catalog) { this.catalog = catalog; }

    @JsonUnwrapped
    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }

    @JsonUnwrapped
    public Supply getSupply() { return supply; }
    public void setSupply(Supply supply) { this.supply = supply; }
}
