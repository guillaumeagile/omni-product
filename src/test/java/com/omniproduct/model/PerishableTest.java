package com.omniproduct.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerishableTest {

    private static final LocalDate SELL_BY = LocalDate.of(2026, 11, 10);
    private static final LocalDate USE_BY = LocalDate.of(2026, 11, 15);

    @Test
    void keepsItsDates() {
        Perishable yoghurt = new Perishable(SELL_BY, USE_BY);

        assertThat(yoghurt.sellByDate()).isEqualTo(SELL_BY);
        assertThat(yoghurt.useByDate()).isEqualTo(USE_BY);
    }

    @Test
    void acceptsAUseByDateEqualToTheSellByDate() {
        assertThat(new Perishable(SELL_BY, SELL_BY).useByDate()).isEqualTo(SELL_BY);
    }

    @Test
    void refusesAMissingSellByDate() {
        assertThatThrownBy(() -> new Perishable(null, USE_BY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sellByDate");
    }

    @Test
    void refusesAMissingUseByDate() {
        assertThatThrownBy(() -> new Perishable(SELL_BY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("useByDate");
    }

    @Test
    void refusesAUseByDateBeforeTheSellByDate() {
        assertThatThrownBy(() -> new Perishable(USE_BY, SELL_BY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("useByDate must not be before sellByDate");
    }

    // Non déterministe, volontairement : isPastSellBy lit l'horloge (LocalDate.now()), le test ne peut que s'y caler.
    // Autour de minuit, le jour de ce test et celui du modèle peuvent différer. HA4.6 demande de sortir l'horloge du
    // modèle ; alors ce test pourra fixer la date du jour au lieu de la deviner.
    @Test
    void isPastSellByOnlyOnceTheSellByDateIsBehindToday() {
        LocalDate today = LocalDate.now();

        assertThat(new Perishable(today.minusDays(1), today.plusDays(3)).isPastSellBy()).isTrue();
        assertThat(new Perishable(today, today.plusDays(3)).isPastSellBy()).isFalse();
        assertThat(new Perishable(today.plusDays(1), today.plusDays(3)).isPastSellBy()).isFalse();
    }
}
