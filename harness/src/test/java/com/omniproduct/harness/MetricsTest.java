package com.omniproduct.harness;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsTest {

    private static final String SHOP = """
            package com.omniproduct.model;

            public class Shop {
                private int stock;

                public int getStock() { return stock; }

                // 1 + if + && + for + ?: = 5
                public int sell(int quantity, boolean open) {
                    if (quantity > 0 && open) {
                        stock = stock - quantity;
                    }
                    for (int i = 0; i < quantity; i++) {
                        stock++;
                    }
                    return stock > 2 ? 3 : 4;
                }

                // 1 + 2 case (default ne compte pas) = 3
                public String label(int kind) {
                    switch (kind) {
                        case 1: return "one";
                        case 2: return "two";
                        default: return "many";
                    }
                }
            }
            """;

    @Test
    void measuresEachMethodWithPmdCyclomaticComplexity() {
        SourceSet sources = SourceSet.ofSources(Map.of("src/main/java/com/omniproduct/model/Shop.java", SHOP), null);

        assertThat(Complexity.of(sources.files())).containsExactlyInAnyOrder(1, 5, 3);
        assertThat(Metrics.of(sources).complexity()).isEqualTo(9);
        assertThat(Metrics.of(sources).maxComplexity()).isEqualTo(5);
    }

    @Test
    void measuresOnlyModelAndService() {
        SourceSet sources = SourceSet.ofSources(Map.of(
                "src/main/java/com/omniproduct/infrastructure/Shop.java", SHOP.replace("com.omniproduct.model", "com.omniproduct.infrastructure")), null);

        assertThat(Metrics.of(sources)).isEqualTo(new Metrics(0, 0, 0));
    }

    @Test
    void countsTokensBySplittingIdentifiersAndPunctuation() {
        assertThat(Tokens.count("getSellByDate();")).isEqualTo(7);
        assertThat(Tokens.count("HTTPServer id")).isEqualTo(3);
        assertThat(Tokens.countChanged(List.of("--- a/X.java", "+++ b/X.java", "@@ -1 +1 @@", "-int a;", "+long a;"))).isEqualTo(6);
    }
}
