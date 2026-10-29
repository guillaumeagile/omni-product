package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.omniproduct.service.ProductService;
import com.omniproduct.service.SupplierService;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    private String id;
    
    private String name;
    @Embedded
    private Contact contact = new Contact();

    @Embedded
    private Location location = new Location();
    
    @OneToMany(mappedBy = "supply.supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products;
    
    public Supplier() {}
    
    public Supplier(String id, String name, String contactEmail, String contactPhone, 
                   String country, String region) {
        this.id = id;
        this.name = name;
        this.contact = new Contact(contactEmail, contactPhone);
        this.location = new Location(country, region);
    }

    public Supplier(String id, String name, ProductService productService, SupplierService supplierService) {
        this.id = id;
        this.name = name;
        this.products = productService.findAll();
        this.location = new Location(null,
            supplierService.getSupplier(id)
                .map(supplier -> supplier.getLocation().getRegion())
                .orElse(null));
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
    
    @JsonUnwrapped
    public Contact getContact() {
        return contact;
    }
    
    public void setContact(Contact contact) {
        this.contact = contact;
    }
    
    @JsonUnwrapped
    public Location getLocation() {
        return location;
    }
    
    public void setLocation(Location location) {
        this.location = location;
    }
    
    public List<Product> getProducts() {
        return products;
    }
    
    public void setProducts(List<Product> products) {
        this.products = products;
    }
}
