package com.omniproduct.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "slug", unique = true, nullable = false))
    private Slug slug;

    @Embedded
    private Price price;

    @ElementCollection
    @CollectionTable(name = "product_discounts", joinColumns = @JoinColumn(name = "product_id"))
    @AttributeOverride(name = "value", column = @Column(name = "discount"))
    private List<Discount> discounts;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @AttributeOverride(name = "key.value", column = @Column(name = "image_key"))
    @AttributeOverride(name = "value.value", column = @Column(name = "image_detail"))
    private Map<ImageKey, ImageDetail> images; // Simplified to Map<ImageKey, ImageDetail> for JPA

    @ElementCollection
    @CollectionTable(name = "product_suppliers", joinColumns = @JoinColumn(name = "product_id"))
    @AttributeOverride(name = "key.value", column = @Column(name = "region"))
    private Map<Region, Supplier> suppliersRegions;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "supplier_id"))
    private SupplierId supplierId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "kilos"))
    private Kilos kilos;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "volume"))
    private Volume volume;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "quantity"))
    private Quantity quantity;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "stock"))
    private Stock stock;

    @Embedded
    private Warehouse warehouse;

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
    public Product(String id, String name, Slug slug, Price price, List<Discount> discounts,
                   Map<ImageKey, ImageDetail> images, Map<Region, Supplier> suppliersRegions, Kilos kilos, Volume volume,
                   Quantity quantity, Stock stock, Warehouse warehouse) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.price = price;
        this.discounts = discounts;
        this.images = images;
        this.suppliersRegions = suppliersRegions;
        this.kilos = kilos;
        this.volume = volume;
        this.quantity = quantity;
        this.stock = stock;
        this.warehouse = warehouse;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Slug getSlug() { return slug; }
    public void setSlug(Slug slug) { this.slug = slug; }

    public Price getPrice() { return price; }
    public void setPrice(Price price) { this.price = price; }

    public List<Discount> getDiscounts() { return discounts; }
    public void setDiscounts(List<Discount> discounts) { this.discounts = discounts; }

    public Map<ImageKey, ImageDetail> getImages() { return images; }
    public void setImages(Map<ImageKey, ImageDetail> images) { this.images = images; }

    public Map<Region, Supplier> getSuppliersRegions() { return suppliersRegions; }
    public void setSuppliersRegions(Map<Region, Supplier> suppliersRegions) { this.suppliersRegions = suppliersRegions; }

    public Kilos getKilos() { return kilos; }
    public void setKilos(Kilos kilos) { this.kilos = kilos; }

    public Volume getVolume() { return volume; }
    public void setVolume(Volume volume) { this.volume = volume; }

    public Quantity getQuantity() { return quantity; }
    public void setQuantity(Quantity quantity) { this.quantity = quantity; }

    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public SupplierId getSupplierId() { return supplierId; }
    public void setSupplierId(SupplierId supplierId) { this.supplierId = supplierId; }

    @Embeddable
    public static class Price {
        @Embedded
        @AttributeOverride(name = "value", column = @Column(name = "base"))
        private BasePrice base;

        @Embedded
        @AttributeOverride(name = "value", column = @Column(name = "tax"))
        private TaxAmount tax;

        @Embedded
        @AttributeOverride(name = "value", column = @Column(name = "tax_rate"))
        private TaxRate taxRate;

        public Price() {}

        public Price(BasePrice base, TaxAmount tax, TaxRate taxRate) {
            this.base = base;
            this.tax = tax;
            this.taxRate = taxRate;
        }

        public BasePrice getBase() { return base; }
        public void setBase(BasePrice base) { this.base = base; }

        public TaxAmount getTax() { return tax; }
        public void setTax(TaxAmount tax) { this.tax = tax; }

        public TaxRate getTaxRate() { return taxRate; }
        public void setTaxRate(TaxRate taxRate) { this.taxRate = taxRate; }
    }

    @Embeddable
    public static class Warehouse {
        @Embedded
        @AttributeOverride(name = "value", column = @Column(name = "location"))
        private Location location;

        public Warehouse() {}

        public Warehouse(Location location) {
            this.location = location;
        }

        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }
    }
}
