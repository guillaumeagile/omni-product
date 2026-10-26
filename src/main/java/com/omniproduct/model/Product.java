package com.omniproduct.model;

import com.omniproduct.model.valueobject.Price;

public interface Product extends Identifiable {

    ProductId getId();

    String getName();

    Price getPrice();

    record Warehouse(Location location) {}

    record Supplier(String name, Siren siren, TvaId tvaId) {}
}
