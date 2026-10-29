package com.omniproduct.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;

    @Embedded
    private Catalog catalog = new Catalog();

    @Embedded
    private Price price;

    @Embedded
    private Inventory inventory = new Inventory();

    @Embedded
    private Sourcing sourcing = new Sourcing();

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

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean hasName() { return name != null && !name.trim().isEmpty(); }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }

        public List<String> getDiscounts() { return discounts; }
        public void setDiscounts(List<String> discounts) { this.discounts = discounts; }

        public Map<String, String> getImages() { return images; }
        public void setImages(Map<String, String> images) { this.images = images; }
    }

    @Embeddable
    public static class Inventory {
        private Integer quantity;
        private Integer stock;

        @Embedded
        private Transport transport = new Transport();

        @Embedded
        private Warehouse warehouse;

        public Inventory() {}

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }

        public Transport getTransport() { return transport; }
        public void setTransport(Transport transport) { this.transport = transport; }

        public Warehouse getWarehouse() { return warehouse; }
        public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    }

    @Embeddable
    public static class Transport {
        private Double kilos;
        private String volume;

        public Transport() {}

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

        public Sourcing() {}

        public com.omniproduct.model.Supplier getSupplier() { return supplier; }
        public void setSupplier(com.omniproduct.model.Supplier supplier) { this.supplier = supplier; }

        public Map<String, Supplier> getSuppliersRegions() { return suppliersRegions; }
        public void setSuppliersRegions(Map<String, Supplier> suppliersRegions) { this.suppliersRegions = suppliersRegions; }
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
    public Product(String id, Catalog catalog, Price price, Inventory inventory, Sourcing sourcing) {
        this.id = id;
        this.catalog = catalog;
        this.price = price;
        this.inventory = inventory;
        this.sourcing = sourcing;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Catalog getCatalog() { return catalog; }
    public void setCatalog(Catalog catalog) { this.catalog = catalog; }

    public Price getPrice() { return price; }
    public void setPrice(Price price) { this.price = price; }

    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

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
