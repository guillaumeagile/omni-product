package com.omniproduct.harness.fixtures.ha7.evasion.model;

import com.omniproduct.harness.fixtures.Identifiable;

import java.util.Optional;

// Volontairement mauvais : référence une entité à travers un type générique de retour (Optional<Supplier>).
public class Product implements Identifiable<String> {
    @Override
    public String id() {
        return "p-1";
    }

    public Optional<Supplier> supplier() {
        return Optional.empty();
    }
}
