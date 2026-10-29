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

    public Supplier(String id, String name, ProductService productService, SupplierService supplierService) {
        this.id = id;
        this.name = name;
        this.products = productService.findAll();
        this.region = supplierService.getSupplier(id).map(Supplier::getLocation).map(Location::region).orElse(null);
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
        return new Contact(contactEmail, contactPhone);
    }

    @JsonUnwrapped
    public void setContact(Contact contact) {
        this.contactEmail = contact.contactEmail();
        this.contactPhone = contact.contactPhone();
    }

    @JsonUnwrapped
    public Location getLocation() {
        return new Location(country, region);
    }

    @JsonUnwrapped
    public void setLocation(Location location) {
        this.country = location.country();
        this.region = location.region();
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public record Contact(String contactEmail, String contactPhone) {}

    public record Location(String country, String region) {}
}
