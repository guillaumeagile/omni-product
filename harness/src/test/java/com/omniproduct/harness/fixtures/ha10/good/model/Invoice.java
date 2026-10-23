package com.omniproduct.harness.fixtures.ha10.good.model;

// Témoin conforme : construite à partir de valeurs et de value objects.
public class Invoice {
    public record Amount(double value) {
    }

    private String id;

    public Invoice(String id, Amount amount) {
        this.id = id;
    }
}
