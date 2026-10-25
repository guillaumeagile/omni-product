package com.omniproduct.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.Map;

// Le catalogue : comment le produit se présente au client.
@Entity
@Table(name = "product_catalogs")
public class ProductCatalog {

    @Id
    private String productId;

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
    private Map<String, String> images;

    public ProductCatalog() {}

    public ProductCatalog(String productId, String slug, List<String> discounts, Map<String, String> images) {
        this.productId = productId;
        this.slug = slug;
        this.discounts = discounts;
        this.images = images;
    }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public List<String> getDiscounts() { return discounts; }
    public void setDiscounts(List<String> discounts) { this.discounts = discounts; }

    public Map<String, String> getImages() { return images; }
    public void setImages(Map<String, String> images) { this.images = images; }
}
