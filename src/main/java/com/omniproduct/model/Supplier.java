package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;

@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    private String id;

    private String name;

    @Embedded
    private Contact contact;

    @Embedded
    private SupplierRegion supplierRegion;

    public Supplier() {}

    public Supplier(String id, String name, String contactEmail, String contactPhone,
                   String country, String region) {
        this.id = id;
        this.name = name;
        this.contact = new Contact(contactEmail, contactPhone);
        this.supplierRegion = new SupplierRegion(country, region);
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
    public SupplierRegion getSupplierRegion() {
        return supplierRegion;
    }

    public void setSupplierRegion(SupplierRegion supplierRegion) {
        this.supplierRegion = supplierRegion;
    }
}
