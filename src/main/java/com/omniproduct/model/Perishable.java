package com.omniproduct.model;

import java.time.LocalDate;

// Exercice étape 2 (HA4.1) : hiérarchie d'héritage volontaire, à remplacer par une ADT (sealed interface + records).
public class Perishable extends Product {

    private final LocalDate sellByDate;
    private final LocalDate useByDate;

    public Perishable(LocalDate sellByDate, LocalDate useByDate) {
        this.sellByDate = sellByDate;
        this.useByDate = useByDate;
    }

    public LocalDate getSellByDate() { return sellByDate; }

    public LocalDate getUseByDate() { return useByDate; }
}
