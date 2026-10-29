package com.omniproduct.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.Map;

@Embeddable
public class Catalog {
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
