package com.omniproduct.harness.fixtures.ha9.evasion.model;

// Sain : implémente Identifiable à travers Entity.
public class Order implements Entity {
    @Override
    public String id() {
        return "o-1";
    }
}
