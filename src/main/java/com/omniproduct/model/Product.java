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
    private Catalogue catalogue = new Catalogue();

    @Embedded
    private Storage storage = new Storage();

    @Embedded
    private Supply supply = new Supply();

    // Default constructor for JPA
    public Product() {}

    // Constructor for testing
    public Product(String id, String name, String slug, Price price, List<String> discounts,
                   Map<String, String> images, Map<String, SupplierIdentity> suppliersRegions, Double weight, String dimensions,
                   Integer quantity, Integer stock, Warehouse warehouse) {
        this.id = id;
        this.price = price;
        this.catalogue = new Catalogue(name, slug, discounts, images);
        this.storage = new Storage(quantity, stock, warehouse, new Parcel(weight, dimensions));
        this.supply = new Supply(null, suppliersRegions);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @JsonUnwrapped
    public Catalogue getCatalogue() { return catalogue; }
    public void setCatalogue(Catalogue catalogue) { this.catalogue = catalogue; }

    public Price getPrice() { return price; }
    public void setPrice(Price price) { this.price = price; }

    @JsonUnwrapped
    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }

    @JsonUnwrapped
    public Supply getSupply() { return supply; }
    public void setSupply(Supply supply) { this.supply = supply; }
}
