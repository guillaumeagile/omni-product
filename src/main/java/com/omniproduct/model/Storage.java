package com.omniproduct.model;

import jakarta.persistence.*;

@Embeddable
public class Storage {
    private Integer quantity;
    private Integer stock;
    
    @Embedded
    private Warehouse warehouse;
    
    public Storage() {}
    
    public Storage(Integer quantity, Integer stock, Warehouse warehouse) {
        this.quantity = quantity;
        this.stock = stock;
        this.warehouse = warehouse;
    }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
}
