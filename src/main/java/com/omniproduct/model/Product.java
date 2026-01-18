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
    
    @Column(unique = true, nullable = false)
    private String slug;
    
    @Embedded
    private Price price;
    
    @ElementCollection
    @CollectionTable(name = "product_discounts", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "discount")
    private List<String> discounts;
    
    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "image_key")
    @Column(name = "image_detail")
    private Map<String, String> images; // Simplified to Map<String, String> for JPA
    
    private Double weight;
    private String dimensions;
    private Integer quantity;
    private Integer stock;
    
    @Embedded
    private Warehouse warehouse;
    
    // Default constructor for JPA
    public Product() {}
    
    // Constructor for testing
    public Product(String id, String name, String slug, Price price, List<String> discounts, 
                   Map<String, String> images, Double weight, String dimensions, 
                   Integer quantity, Integer stock, Warehouse warehouse) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.price = price;
        this.discounts = discounts;
        this.images = images;
        this.weight = weight;
        this.dimensions = dimensions;
        this.quantity = quantity;
        this.stock = stock;
        this.warehouse = warehouse;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    
    public Price getPrice() { return price; }
    public void setPrice(Price price) { this.price = price; }
    
    public List<String> getDiscounts() { return discounts; }
    public void setDiscounts(List<String> discounts) { this.discounts = discounts; }
    
    public Map<String, String> getImages() { return images; }
    public void setImages(Map<String, String> images) { this.images = images; }
    
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    
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
