package com.omniproduct.harness.fixtures.ha7.evasion.model;

import com.omniproduct.harness.fixtures.Identifiable;

// Volontairement mauvais : relie Supplier à travers le value object Offer (un record n'est pas une entité, mais il
// porte une entité : la dépendance fuit).
public class Basket implements Identifiable<String> {
    @Override
    public String id() {
        return "b-1";
    }

    public Offer offer() {
        return null;
    }
}
