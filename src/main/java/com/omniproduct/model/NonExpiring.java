package com.omniproduct.model;

import java.time.LocalDate;

// Exercice étape 2 (HA4.1) : hiérarchie d'héritage volontaire, à remplacer par une ADT (sealed interface + records).
public class NonExpiring extends Product {

    private final LocalDate bestBeforeDate;

    public NonExpiring(LocalDate bestBeforeDate) {
        this.bestBeforeDate = bestBeforeDate;
    }

    public LocalDate getBestBeforeDate() { return bestBeforeDate; }
}
