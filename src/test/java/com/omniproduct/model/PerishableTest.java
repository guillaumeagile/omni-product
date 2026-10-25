package com.omniproduct.model;

import com.omniproduct.model.valueobject.Price;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerishableTest {

    private static final ProductId ID = new ProductId(UUID.fromString("0190a6e2-7b3c-7d4e-8f00-000000000001"));
    private static final Price PRICE = new Price(new Amount(10.0), new Amount(2.0), new TaxRate(0.2));
    private static final LocalDate SELL_BY = LocalDate.of(2026, 11, 10);
    private static final LocalDate USE_BY = LocalDate.of(2026, 11, 15);

    @Test
    void keepsItsDatesAndItsProductData() {
        Perishable yoghurt = new Perishable(ID, "Yoghurt", PRICE, SELL_BY, USE_BY);

        assertThat(yoghurt.getId()).isEqualTo(ID);
        assertThat(yoghurt.getName()).isEqualTo("Yoghurt");
        assertThat(yoghurt.getPrice()).isEqualTo(PRICE);
        assertThat(yoghurt.getSellByDate()).isEqualTo(SELL_BY);
        assertThat(yoghurt.getUseByDate()).isEqualTo(USE_BY);
    }

    @Test
    void acceptsAUseByDateEqualToTheSellByDate() {
        assertThat(new Perishable(ID, "Yoghurt", PRICE, SELL_BY, SELL_BY).getUseByDate()).isEqualTo(SELL_BY);
    }

    @Test
    void refusesAMissingSellByDate() {
        assertThatThrownBy(() -> new Perishable(ID, "Yoghurt", PRICE, null, USE_BY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sellByDate");
    }

    @Test
    void refusesAMissingUseByDate() {
        assertThatThrownBy(() -> new Perishable(ID, "Yoghurt", PRICE, SELL_BY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("useByDate");
    }

    @Test
    void refusesAUseByDateBeforeTheSellByDate() {
        assertThatThrownBy(() -> new Perishable(ID, "Yoghurt", PRICE, USE_BY, SELL_BY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("useByDate must not be before sellByDate");
    }

    // Non déterministe, volontairement : isPastSellBy lit l'horloge (LocalDate.now()), le test ne peut que s'y caler.
    // Autour de minuit, le jour de ce test et celui du modèle peuvent différer. HA4.6 demande de sortir l'horloge du
    // modèle ; alors ce test pourra fixer la date du jour au lieu de la deviner.
    @Test
    void isPastSellByOnlyOnceTheSellByDateIsBehindToday() {
        LocalDate today = LocalDate.now();

        assertThat(new Perishable(ID, "Yoghurt", PRICE, today.minusDays(1), today.plusDays(3)).isPastSellBy()).isTrue();
        assertThat(new Perishable(ID, "Yoghurt", PRICE, today, today.plusDays(3)).isPastSellBy()).isFalse();
        assertThat(new Perishable(ID, "Yoghurt", PRICE, today.plusDays(1), today.plusDays(3)).isPastSellBy()).isFalse();
    }
}
