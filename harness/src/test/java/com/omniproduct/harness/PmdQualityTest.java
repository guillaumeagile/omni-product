package com.omniproduct.harness;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PmdQualityTest {

    // Java 21 : sealed interface, records, switch à motifs. CK 0.7.0 n'y voit aucun record ; PMD les mesure.
    private static final String SHELF = """
            package com.omniproduct.model;

            import java.time.LocalDate;
            import java.util.List;

            public sealed interface ShelfLife permits Perishable, NonExpiring {
                static boolean canSell(ShelfLife life, LocalDate today) {
                    return switch (life) {
                        case Perishable p -> !today.isAfter(p.sellBy());
                        case NonExpiring n -> true;
                    };
                }
            }

            record Perishable(LocalDate sellBy, LocalDate useBy) implements ShelfLife {
                boolean safe(LocalDate today) {
                    if (today != null) {
                        if (today.isBefore(useBy)) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            record NonExpiring(LocalDate bestBefore) implements ShelfLife {
            }

            class Shelf {
                private int count;
                private final List<ShelfLife> lives = List.of();

                long sellable(LocalDate today) {
                    count = 0;
                    lives.forEach(life -> lives.stream().filter(other -> other == life).forEach(other -> count++));
                    for (ShelfLife life : lives) {
                        count += 1;
                    }
                    return count;
                }
            }
            """;

    @Test
    void measuresRecordsAndJava21WithPmd() {
        SourceSet sources = SourceSet.ofSources(Map.of("src/main/java/com/omniproduct/model/ShelfLife.java", SHELF), null);

        PmdQuality quality = Metrics.of(sources).quality();
        CkMetrics ck = Metrics.of(sources).ck();

        // Les 2 records et la classe (l'interface ne compte pas) ; CK ne voit que la classe.
        assertThat(quality.types()).isEqualTo(3);
        assertThat(ck.classes()).isEqualTo(1);
        // Seul count est mutable (les composants de record sont final).
        assertThat(quality.mutableFields()).isEqualTo(1);
        // count = 0, count++ (dans une lambda imbriquée), count += 1.
        assertThat(quality.assignments()).isEqualTo(3);
        assertThat(quality.lambdas()).isEqualTo(3);
        assertThat(quality.loops()).isEqualTo(1);
        // safe : if (+1) imbriqué dans un if (+1, +1 d'imbrication) = 3, la plus forte.
        assertThat(quality.maxCognitive()).isEqualTo(3);
    }
}
