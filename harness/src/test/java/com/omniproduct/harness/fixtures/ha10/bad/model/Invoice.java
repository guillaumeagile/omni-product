package com.omniproduct.harness.fixtures.ha10.bad.model;

// Volontairement mauvais : une entité qui reçoit un service à la construction (HA10).
public class Invoice {
    private String id;

    public Invoice(String id) {
        this.id = id;
    }

    public Invoice(String id, PricingService pricing) {
        this.id = id;
    }
}
