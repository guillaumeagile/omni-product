package com.omniproduct.model;

import java.util.List;
import java.util.Map;

// Le catalogue : comment le produit se présente au client.
public class ProductCatalog {

    private String slug;
    private List<String> discounts;
    private Map<String, String> images;

    public ProductCatalog() {}

    public ProductCatalog(String slug, List<String> discounts, Map<String, String> images) {
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
