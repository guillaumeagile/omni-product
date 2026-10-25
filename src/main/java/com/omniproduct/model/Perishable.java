package com.omniproduct.model;

import java.time.LocalDate;
import java.util.Objects;

public record Perishable(LocalDate sellByDate, LocalDate useByDate) implements ShelfLife {

    public Perishable {
        Objects.requireNonNull(sellByDate, "sellByDate must not be null");
        Objects.requireNonNull(useByDate, "useByDate must not be null");
        if (useByDate.isBefore(sellByDate)) {
            throw new IllegalArgumentException("useByDate must not be before sellByDate");
        }
    }

    // Dérive l'état du jour : pratique, mais le modèle ne doit pas lire l'horloge (HA4.6).
    public boolean isPastSellBy() { return LocalDate.now().isAfter(sellByDate); }
}
