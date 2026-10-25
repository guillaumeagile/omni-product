package com.omniproduct.model;

import jakarta.persistence.*;

// Le prix du produit.
@Entity
@Table(name = "product_prices")
public class ProductPrice {

    @Id
    private String productId;

    @Embedded
    private Product.Price price;

    public ProductPrice() {}

    public ProductPrice(String productId, Product.Price price) {
        this.productId = productId;
        this.price = price;
    }

    public Product.Price getPrice() { return price; }
    public void setPrice(Product.Price price) { this.price = price; }
}
