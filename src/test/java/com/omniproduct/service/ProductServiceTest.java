package com.omniproduct.service;

import com.omniproduct.model.Amount;
import com.omniproduct.model.Perishable;
import com.omniproduct.model.Product;
import com.omniproduct.model.ProductId;
import com.omniproduct.model.ProductStock;
import com.omniproduct.model.Quantity;
import com.omniproduct.model.TaxRate;
import com.omniproduct.model.valueobject.Price;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceTest {

    private static final ProductService SERVICE = new ProductService();
    private static final Price PRICE = new Price(new Amount(10.0), new Amount(2.0), new TaxRate(0.2));
    private static final LocalDate SELL_BY = LocalDate.of(2026, 11, 10);
    private static final ProductStock STOCK = new ProductStock(new Quantity(3), new Quantity(1), new Product.Warehouse(null));

    private static final Product YOGHURT = new Product(
            new ProductId(UUID.fromString("0190a6e2-7b3c-7d4e-8f00-000000000001")),
            "Yoghurt",
            PRICE,
            Perishable.of(SELL_BY, SELL_BY.plusDays(5)).get());

    @Test
    void sellsOneUnitWhileTheProductCanBeSold() {
        ProductStock sold = SERVICE.sellOne(YOGHURT, STOCK, SELL_BY).get();

        assertThat(sold.stock().value()).isEqualTo(2);
    }

    @Test
    void refusesTheSaleOnceThePerishableIsPastSellBy() {
        assertThat(SERVICE.sellOne(YOGHURT, STOCK, SELL_BY.plusDays(1)).isEmpty()).isTrue();
    }
}
