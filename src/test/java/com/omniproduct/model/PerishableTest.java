package com.omniproduct.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PerishableTest {

    private static final LocalDate SELL_BY = LocalDate.of(2026, 11, 10);
    private static final LocalDate USE_BY = LocalDate.of(2026, 11, 15);

    @Test
    void keepsItsDates() {
        Perishable yoghurt = Perishable.of(SELL_BY, USE_BY).get();

        assertThat(yoghurt.sellByDate()).isEqualTo(SELL_BY);
        assertThat(yoghurt.useByDate()).isEqualTo(USE_BY);
    }

    @Test
    void acceptsAUseByDateEqualToTheSellByDate() {
        assertThat(Perishable.of(SELL_BY, SELL_BY).get().useByDate()).isEqualTo(SELL_BY);
    }

    @Test
    void refusesAMissingSellByDate() {
        assertThat(Perishable.of(null, USE_BY).isEmpty()).isTrue();
    }

    @Test
    void refusesAMissingUseByDate() {
        assertThat(Perishable.of(SELL_BY, null).isEmpty()).isTrue();
    }

    @Test
    void refusesAUseByDateBeforeTheSellByDate() {
        assertThat(Perishable.of(USE_BY, SELL_BY).isEmpty()).isTrue();
    }

    @Test
    void isPastSellByOnlyOnceTheSellByDateIsBehindTheProvidedToday() {
        LocalDate today = LocalDate.of(2026, 11, 10);
        TimeServiceProvider fakeToday = () -> today;

        assertThat(Perishable.of(today.minusDays(1), today.plusDays(3)).get().isPastSellBy(fakeToday)).isTrue();
        assertThat(Perishable.of(today, today.plusDays(3)).get().isPastSellBy(fakeToday)).isFalse();
        assertThat(Perishable.of(today.plusDays(1), today.plusDays(3)).get().isPastSellBy(fakeToday)).isFalse();
    }
}
