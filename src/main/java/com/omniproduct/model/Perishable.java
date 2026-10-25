package com.omniproduct.model;

import io.vavr.control.Option;

import java.time.LocalDate;

public record Perishable(LocalDate sellByDate, LocalDate useByDate) implements ShelfLife {

    public static Option<Perishable> of(LocalDate sellByDate, LocalDate useByDate) {
        if (sellByDate == null || useByDate == null || useByDate.isBefore(sellByDate)) {
            return Option.none();
        }
        return Option.of(new Perishable(sellByDate, useByDate));
    }

    // Dérive l'état du jour : pratique, mais le modèle ne doit pas lire l'horloge (HA4.6).
    public boolean isPastSellBy() { return LocalDate.now().isAfter(sellByDate); }
}
