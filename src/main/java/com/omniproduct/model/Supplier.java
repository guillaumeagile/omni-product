package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class Supplier implements Identifiable {
    private final SupplierId id;

    private final String name;

    private final Contact contact;

    private final Address address;

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

    public String getName() {
        return name;
    }

    @JsonUnwrapped
    public Contact getContact() {
        return contact;
    }

    @JsonUnwrapped
    public Address getAddress() {
        return address;
    }
}
