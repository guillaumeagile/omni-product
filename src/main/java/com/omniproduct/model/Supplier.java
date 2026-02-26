package com.omniproduct.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    private String id;
    
    private String name;
    private String contactEmail;
    private String contactPhone;
    private String country;
    private String region;
    
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products;
    
    public Supplier() {}
    
    public Supplier(String id, String name, String contactEmail, String contactPhone, 
                   String country, String region) {
        this.id = id;
        this.name = name;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.country = country;
        this.region = region;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getContactEmail() {
        return contactEmail;
    }
    
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
    
    public String getContactPhone() {
        return contactPhone;
    }
    
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public List<Product> getProducts() {
        return products;
    }
    
    public void setProducts(List<Product> products) {
        this.products = products;
    }
}
