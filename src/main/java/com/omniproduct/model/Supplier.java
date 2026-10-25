package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class Supplier implements Identifiable {
    private SupplierId id;

    private String name;

    private Contact contact;

    private Address address;

    public Supplier() {}

    public Supplier(SupplierId id, String name, Contact contact, Address address) {
        this.id = id;
        this.name = name;
        this.contact = contact;
        this.address = address;
    }

    public record Contact(Email contactEmail, Phone contactPhone) {}

    public record Address(Country country, Region region) {}

    public SupplierId getId() {
        return id;
    }

    public void setId(SupplierId id) {
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
    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
