package com.omniproduct.model;

import com.omniproduct.model.valueobject.Price;

import java.time.LocalDate;

public class NonExpiring implements Product {

    private final ProductId id;
    private final String name;
    private final Price price;
    private final LocalDate bestBeforeDate;

    public NonExpiring(ProductId id, String name, Price price, LocalDate bestBeforeDate) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.bestBeforeDate = bestBeforeDate;
    }

    @Override
    public ProductId getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public Price getPrice() { return price; }

    public LocalDate getBestBeforeDate() { return bestBeforeDate; }
}
