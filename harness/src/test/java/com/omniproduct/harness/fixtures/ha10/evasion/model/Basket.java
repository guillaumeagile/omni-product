package com.omniproduct.harness.fixtures.ha10.evasion.model;

import java.util.function.Supplier;

// Volontairement mauvais : aucun paramètre ne s'appelle « Service », mais ce sont des collaborateurs (HA10).
public class Basket {
    private String basketId;

    public Basket(String basketId, PriceManager prices) {
        this.basketId = basketId;
    }

    public Basket(String basketId, Supplier<StockClient> stock) {
        this.basketId = basketId;
    }

    public Basket(String basketId, Wiring wiring) {
        this.basketId = basketId;
    }
}
