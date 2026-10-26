package com.omniproduct.model;

import io.vavr.control.Option;

import java.time.LocalDate;

public record Perishable(LocalDate sellByDate, LocalDate useByDate) implements ShelfLife {

    public static Option<Perishable> of(LocalDate sellByDate, LocalDate useByDate) {
        return Option.of(sellByDate)
                .flatMap(sell -> Option.of(useByDate).map(use -> new Perishable(sell, use)))
                .filter(perishable -> !perishable.useByDate().isBefore(perishable.sellByDate()));
    }

    public boolean isPastSellBy(TimeServiceProvider time) { return time.today().isAfter(sellByDate); }
}
