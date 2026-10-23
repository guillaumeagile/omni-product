package com.omniproduct.harness.fixtures.ha7.evasion.model;

import com.omniproduct.harness.fixtures.Identifiable;

public class Supplier implements Identifiable<String> {
    @Override
    public String id() {
        return "s-1";
    }
}
