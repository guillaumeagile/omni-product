package com.omniproduct.model;

import java.time.LocalDate;

// Exercice étape 2 (HA4.1) : hiérarchie d'héritage volontaire, à remplacer par une ADT (sealed interface + records).
public class NonExpiring extends Product {

    private final LocalDate bestBeforeDate;

    public NonExpiring(String id, String name, Price price, LocalDate bestBeforeDate) {
        super(id, name, price);
        this.bestBeforeDate = bestBeforeDate;
    }

    public LocalDate getBestBeforeDate() { return bestBeforeDate; }
}
