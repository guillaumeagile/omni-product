package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class Supplier implements Identifiable {

    private final SupplierId id;
    private final String name;
    @JsonUnwrapped
    private final Contact contact;
    @JsonUnwrapped
    private final Address address;

    public Supplier(SupplierId id, String name, Contact contact, Address address) {
        this.id = id;
        this.name = name;
        this.contact = contact;
        this.address = address;
    }

    public SupplierId getId() { return id; }

    public String getName() { return name; }

    public Contact getContact() { return contact; }

    public Address getAddress() { return address; }

    public record Contact(Email contactEmail, Phone contactPhone) {}

    public record Address(Country country, Region region) {}
}
