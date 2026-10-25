package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import java.util.List;
import java.util.Map;

// Product ne fait qu'assembler ses préoccupations : catalogue, prix, stock, approvisionnement.
// Chaque préoccupation vit dans son propre objet ; le JSON reste à plat grâce à @JsonUnwrapped.
@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;

    @Embedded
    private Catalog catalog;

    @Embedded
    private Price price;

    @Embedded
    private Inventory inventory;

    @Embedded
    private Sourcing sourcing;

    @Embeddable
    public static class Catalog {
        @Column(nullable = false)
        private String name;

        @Column(unique = true, nullable = false)
        private String slug;

        @ElementCollection
        @CollectionTable(name = "product_discounts", joinColumns = @JoinColumn(name = "product_id"))
        @Column(name = "discount")
        private List<String> discounts;

        @ElementCollection
        @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
        @MapKeyColumn(name = "image_key")
        @Column(name = "image_detail")
        private Map<String, String> images; // Simplified to Map<String, String> for JPA

        public Catalog() {}

        public Catalog(String name, String slug, List<String> discounts, Map<String, String> images) {
            this.name = name;
            this.slug = slug;
            this.discounts = discounts;
            this.images = images;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }

        public List<String> getDiscounts() { return discounts; }
        public void setDiscounts(List<String> discounts) { this.discounts = discounts; }

        public Map<String, String> getImages() { return images; }
        public void setImages(Map<String, String> images) { this.images = images; }
    }

    @Embeddable
    public static class Inventory {
        private Integer stock;
        private Integer quantity;
        private Double kilos;
        private String volume;

        public Inventory() {}

        public Inventory(Integer stock, Integer quantity, Double kilos, String volume) {
            this.stock = stock;
            this.quantity = quantity;
            this.kilos = kilos;
            this.volume = volume;
        }

        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Double getKilos() { return kilos; }
        public void setKilos(Double kilos) { this.kilos = kilos; }

        public String getVolume() { return volume; }
        public void setVolume(String volume) { this.volume = volume; }
    }

    @Embeddable
    public static class Sourcing {
        @ManyToOne
        @JoinColumn(name = "supplier_id")
        private com.omniproduct.model.Supplier supplier;

        @ElementCollection
        @CollectionTable(name = "product_suppliers", joinColumns = @JoinColumn(name = "product_id"))
        @MapKeyColumn(name = "region")
        private Map<String, Supplier> suppliersRegions;

        @Embedded
        private Warehouse warehouse;

        public Sourcing() {}

        public Sourcing(com.omniproduct.model.Supplier supplier, Map<String, Supplier> suppliersRegions, Warehouse warehouse) {
            this.supplier = supplier;
            this.suppliersRegions = suppliersRegions;
            this.warehouse = warehouse;
        }

        public com.omniproduct.model.Supplier getSupplier() { return supplier; }
        public void setSupplier(com.omniproduct.model.Supplier supplier) { this.supplier = supplier; }

        public Map<String, Supplier> getSuppliersRegions() { return suppliersRegions; }
        public void setSuppliersRegions(Map<String, Supplier> suppliersRegions) { this.suppliersRegions = suppliersRegions; }

        public Warehouse getWarehouse() { return warehouse; }
        public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    }

    @Embeddable
    public static class Supplier {
        private String name;
        private String siren;
        private String tvaId;

        public Supplier() {}

        public Supplier(String name, String siren, String tvaId) {
            this.name = name;
            this.siren = siren;
            this.tvaId = tvaId;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSiren() { return siren; }
        public void setSiren(String siren) { this.siren = siren; }

        public String getTvaId() { return tvaId; }
        public void setTvaId(String tvaId) { this.tvaId = tvaId; }
    }

    // Default constructor for JPA
    public Product() {}

    // Constructor for testing
    public Product(String id, String name, String slug, Price price, List<String> discounts,
                   Map<String, String> images, Map<String, Supplier> suppliersRegions, Double weight, String dimensions,
                   Integer quantity, Integer stock, Warehouse warehouse) {
        this.id = id;
        this.catalog = new Catalog(name, slug, discounts, images);
        this.price = price;
        this.inventory = new Inventory(stock, quantity, weight, dimensions);
        this.sourcing = new Sourcing(null, suppliersRegions, warehouse);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @JsonUnwrapped
    public Catalog getCatalog() { return catalog; }
    public void setCatalog(Catalog catalog) { this.catalog = catalog; }

    public Price getPrice() { return price; }
    public void setPrice(Price price) { this.price = price; }

    @JsonUnwrapped
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @JsonUnwrapped
    public Sourcing getSourcing() { return sourcing; }
    public void setSourcing(Sourcing sourcing) { this.sourcing = sourcing; }

    @Embeddable
    public static class Price {
        private Double base;
        private Double tax;
        private Double taxRate;

        public Price() {}

        public Price(Double base, Double tax, Double taxRate) {
            this.base = base;
            this.tax = tax;
            this.taxRate = taxRate;
        }

        public Double getBase() { return base; }
        public void setBase(Double base) { this.base = base; }

        public Double getTax() { return tax; }
        public void setTax(Double tax) { this.tax = tax; }

        public Double getTaxRate() { return taxRate; }
        public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
    }

    @Embeddable
    public static class Warehouse {
        private String location;

        public Warehouse() {}

        public Warehouse(String location) {
            this.location = location;
        }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }
}
