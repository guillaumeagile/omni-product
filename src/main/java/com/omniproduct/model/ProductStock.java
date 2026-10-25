package com.omniproduct.model;

import jakarta.persistence.*;

// Le stock : combien d'unités, et où elles sont entreposées.
@Entity
@Table(name = "product_stocks")
public class ProductStock {

    @Id
    private String productId;

    private Integer stock;
    private Integer quantity;

    @Embedded
    private Product.Warehouse warehouse;

    public ProductStock() {}

    public ProductStock(String productId, Integer stock, Integer quantity, Product.Warehouse warehouse) {
        this.productId = productId;
        this.stock = stock;
        this.quantity = quantity;
        this.warehouse = warehouse;
    }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Product.Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Product.Warehouse warehouse) { this.warehouse = warehouse; }
}
