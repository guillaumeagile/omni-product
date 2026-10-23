package com.omniproduct.harness.fixtures.ha7.good.model;

import java.util.List;

// Témoin conforme : références par identifiant, et value object libre.
public class Order {
    public record Address(String street) {
    }

    private String id;
    private String customerId;
    private List<String> watcherIds;
    private Address address;

    public void assign(String otherCustomerId) {
    }
}
