package com.omniproduct.model;

import com.omniproduct.model.valueobject.Price;
import io.vavr.control.Option;

import java.time.LocalDate;

public class Product implements Identifiable {

    private final ProductId id;
    private final String name;
    private final Price price;
    private final ShelfLife shelfLife;

    private Product(ProductId id, String name, Price price, ShelfLife shelfLife) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.shelfLife = shelfLife;
    }

    public static Option<Product> of(ProductId id, String name, Price price, ShelfLife shelfLife) {
        return Option.of(shelfLife).map(validShelfLife -> new Product(id, name, price, validShelfLife));
    }

    public ProductId getId() { return id; }

    public String getName() { return name; }

    public Price getPrice() { return price; }

    public ShelfLife getShelfLife() { return shelfLife; }

    public boolean canSellOn(LocalDate today) {
        return switch (shelfLife) {
            case Perishable perishable -> !today.isAfter(perishable.sellByDate());
            case NonExpiring nonExpiring -> true;
        };
    }

    public record Warehouse(Location location) {}

    public record Supplier(String name, Siren siren, TvaId tvaId) {}
}
