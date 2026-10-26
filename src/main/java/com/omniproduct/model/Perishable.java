package com.omniproduct.model;

import com.omniproduct.model.valueobject.Price;
import com.omniproduct.model.valueobject.ShelfLife;

import java.time.LocalDate;
import java.util.Objects;

public class Perishable implements Product {

    private final ProductId id;
    private final String name;
    private final Price price;
    private final ShelfLife shelfLife;

    public Perishable(ProductId id, String name, Price price, LocalDate sellByDate, LocalDate useByDate) {
        this.id = id;
        this.name = name;
        this.price = price;
        LocalDate checkedSellByDate = Objects.requireNonNull(sellByDate, "sellByDate must not be null");
        LocalDate checkedUseByDate = Objects.requireNonNull(useByDate, "useByDate must not be null");
        if (checkedUseByDate.isBefore(checkedSellByDate)) {
            throw new IllegalArgumentException("useByDate must not be before sellByDate");
        }
        this.shelfLife = new ShelfLife(checkedSellByDate, checkedUseByDate);
    }

    @Override
    public ProductId getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public Price getPrice() { return price; }

    public ShelfLife getShelfLife() { return shelfLife; }

    // Dérive l'état du jour : pratique, mais le modèle ne doit pas lire l'horloge (HA4.6).
    public boolean isPastSellBy() { return LocalDate.now().isAfter(shelfLife.sellByDate()); }
}
