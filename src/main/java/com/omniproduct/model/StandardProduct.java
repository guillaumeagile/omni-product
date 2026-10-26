package com.omniproduct.model;

import com.omniproduct.model.valueobject.Price;

public class StandardProduct implements Product {

    private final ProductId id;
    private final String name;
    private final Price price;

    public StandardProduct(ProductId id, String name, Price price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    @Override
    public ProductId getId() { return id; }

    @Override
    public String getName() { return name; }

    @Override
    public Price getPrice() { return price; }
}
