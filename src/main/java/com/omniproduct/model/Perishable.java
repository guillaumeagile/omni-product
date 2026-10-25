package com.omniproduct.model;

import java.time.LocalDate;
import java.util.Objects;

// Exercice étape 2 (HA4.1) : hiérarchie d'héritage volontaire, à remplacer par une ADT (sealed interface + records).
public class Perishable extends Product {

    private final LocalDate sellByDate;
    private final LocalDate useByDate;

    public Perishable(LocalDate sellByDate, LocalDate useByDate) {
        this.sellByDate = Objects.requireNonNull(sellByDate, "sellByDate must not be null");
        this.useByDate = Objects.requireNonNull(useByDate, "useByDate must not be null");
        if (useByDate.isBefore(sellByDate)) {
            throw new IllegalArgumentException("useByDate must not be before sellByDate");
        }
    }

    public LocalDate getSellByDate() { return sellByDate; }

    public LocalDate getUseByDate() { return useByDate; }
}
