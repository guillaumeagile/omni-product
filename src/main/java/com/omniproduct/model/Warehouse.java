package com.omniproduct.model;

import jakarta.persistence.*;

@Embeddable
public class Warehouse {
    private String location;
    
    public Warehouse() {}
    
    public Warehouse(String location) {
        this.location = location;
    }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
