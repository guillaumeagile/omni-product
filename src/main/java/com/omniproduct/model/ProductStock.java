package com.omniproduct.model;

// Le stock : combien d'unités, et où elles sont entreposées.
public class ProductStock {

    private Integer stock;
    private Integer quantity;
    private Product.Warehouse warehouse;

    public ProductStock() {}

    public ProductStock(Integer stock, Integer quantity, Product.Warehouse warehouse) {
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
