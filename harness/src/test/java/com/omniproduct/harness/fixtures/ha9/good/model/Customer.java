package com.omniproduct.harness.fixtures.ha9.good.model;

import com.omniproduct.harness.fixtures.Identifiable;

// Témoin conforme : une entité Identifiable, et un record (value object) exempté.
public class Customer implements Identifiable<String> {
    public record Address(String street) {
    }

    @Override
    public String id() {
        return "c1";
    }
}
