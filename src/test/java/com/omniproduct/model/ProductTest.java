package com.omniproduct.model;

import com.omniproduct.model.valueobject.Price;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    private static final UUID UUID_1 = UUID.fromString("0190a6e2-7b3c-7d4e-8f00-000000000001");
    private static final Price PRICE = new Price(new Amount(10.0), new TaxRate(0.2));

    @Test
    void identifiersAreEqualWhenTheirValuesAre() {
        assertThat(new ProductId(UUID_1)).isEqualTo(new ProductId(UUID_1));
        assertThat(new ProductId(UUID_1)).isNotEqualTo(new ProductId(UUID.randomUUID()));
        assertThat(new Siren("552100554")).isEqualTo(new Siren("552100554"));
        assertThat(new TvaId("FR40552100554")).isNotEqualTo(new TvaId("FR00000000000"));
    }

    @Test
    void conceptsAreValuesComparedByContent() {
        assertThat(PRICE).isEqualTo(new Price(new Amount(10.0), new TaxRate(0.2)));
        assertThat(PRICE).isNotEqualTo(new Price(new Amount(10.0), new TaxRate(0.055)));
        assertThat(new Product.Supplier("Danone", new Siren("552032534"), new TvaId("FR27552032534")))
                .isEqualTo(new Product.Supplier("Danone", new Siren("552032534"), new TvaId("FR27552032534")));
    }

    @Test
    void aNonExpiringProductKeepsItsBestBeforeDate() {
        LocalDate bestBefore = LocalDate.of(2027, 6, 1);
        NonExpiring rice = new NonExpiring(new ProductId(UUID_1), "Rice", PRICE, bestBefore);

        assertThat(rice.getName()).isEqualTo("Rice");
        assertThat(rice.getPrice()).isEqualTo(PRICE);
        assertThat(rice.getBestBeforeDate()).isEqualTo(bestBefore);
    }
}
