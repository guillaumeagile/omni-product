package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;

@Embeddable
public class Storage {
    private Integer quantity;
    private Integer stock;
    
    @Embedded
    private Warehouse warehouse;

    @Embedded
    @JsonUnwrapped
    private Parcel parcel;
    
    public Storage() {
        this.parcel = new Parcel();
    }
    
    public Storage(Integer quantity, Integer stock, Warehouse warehouse, Parcel parcel) {
        this.quantity = quantity;
        this.stock = stock;
        this.warehouse = warehouse;
        this.parcel = parcel;
    }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public Parcel getParcel() { return parcel; }
    public void setParcel(Parcel parcel) { this.parcel = parcel; }
}
