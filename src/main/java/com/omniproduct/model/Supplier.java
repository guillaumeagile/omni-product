package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record Supplier(SupplierId id, String name, @JsonUnwrapped Contact contact, @JsonUnwrapped Address address) {

    public record Contact(Email contactEmail, Phone contactPhone) {}

    public record Address(Country country, Region region) {}
}
