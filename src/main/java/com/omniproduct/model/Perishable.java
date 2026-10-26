package com.omniproduct.model;

import com.omniproduct.model.valueobject.Price;

import java.time.LocalDate;
import java.util.Objects;

public class Perishable implements Product {

    private final ProductId id;
    private final String name;
    private final Price price;
    private final LocalDate sellByDate;
    private final LocalDate useByDate;

    public Perishable(ProductId id, String name, Price price, LocalDate sellByDate, LocalDate useByDate) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.sellByDate = Objects.requireNonNull(sellByDate, "sellByDate must not be null");
        this.useByDate = Objects.requireNonNull(useByDate, "useByDate must not be null");
        if (useByDate.isBefore(sellByDate)) {
            throw new IllegalArgumentException("useByDate must not be before sellByDate");
        }
    }

    @Override
    public ProductId getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public Price getPrice() { return price; }

    public LocalDate getSellByDate() { return sellByDate; }

    public LocalDate getUseByDate() { return useByDate; }

    // Dérive l'état du jour : pratique, mais le modèle ne doit pas lire l'horloge (HA4.6).
    public boolean isPastSellBy() { return LocalDate.now().isAfter(sellByDate); }
}
