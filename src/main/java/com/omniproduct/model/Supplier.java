package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class Supplier implements Identifiable {
    private String id;

    private String name;

    private Contact contact;

    private Address address;

    public Supplier() {}

    public Supplier(String id, String name, String contactEmail, String contactPhone,
                   String country, String region) {
        this.id = id;
        this.name = name;
        this.contact = new Contact(contactEmail, contactPhone);
        this.address = new Address(country, region);
    }

    public record Contact(String contactEmail, String contactPhone) {}

    public record Address(String country, String region) {}

    @Override
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
    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
