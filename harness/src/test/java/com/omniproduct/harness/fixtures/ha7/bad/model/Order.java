package com.omniproduct.harness.fixtures.ha7.bad.model;

import java.util.List;

// Volontairement mauvais : une entité qui tient d'autres entités (HA7).
public class Order {
    private String id;
    private Customer customer;
    private List<Customer> watchers;

    public void assign(Customer other) {
    }
}
