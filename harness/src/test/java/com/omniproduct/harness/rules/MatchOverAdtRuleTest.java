package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MatchOverAdtRuleTest {

    private static final String SHELF = """
            package demo.model;

            public sealed interface Shelf permits Perishable, NonExpiring {
            }

            record Perishable(String sellBy) implements Shelf {
            }

            record NonExpiring(String bestBefore) implements Shelf {
            }
            """;

    private static List<Violation> check(String user) {
        return new MatchOverAdtRule(pkg -> pkg.contains(".model"))
                .check(SourceSet.ofSources(Map.of("Shelf.java", SHELF, "User.java", user), null));
    }

    @Test
    void acceptsAnExhaustiveSwitchOverTheAlternatives() {
        String user = """
                package demo.model;

                class User {
                    String label(Shelf shelf) {
                        return switch (shelf) {
                            case Perishable p -> "sell by " + p.sellBy();
                            case NonExpiring n -> "best before " + n.bestBefore();
                        };
                    }
                }
                """;

        assertThat(check(user)).isEmpty();
    }

    @Test
    void flagsInstanceofOnAnAlternativeWithItsLine() {
        String user = """
                package demo.model;

                class User {
                    boolean perishable(Shelf shelf) {
                        return shelf instanceof Perishable;
                    }
                    String label(Shelf shelf) {
                        return switch (shelf) {
                            case Perishable p -> "a";
                            case NonExpiring n -> "b";
                        };
                    }
                }
                """;

        assertThat(check(user)).anySatisfy(v -> {
            assertThat(v.line()).isEqualTo(5);
            assertThat(v.message()).contains("'instanceof Perishable'");
        });
    }

    private static final String SALE = """
            package demo.model;

            public sealed interface SaleDecision permits Sellable, PastSellByDate {
            }

            record Sellable() implements SaleDecision {
            }

            record PastSellByDate(String since) implements SaleDecision {
            }

            class Product {
                SaleDecision canSell(String today) { return new Sellable(); }
            }
            """;

    private static List<Violation> checkSale(String user) {
        return new MatchOverAdtRule(pkg -> pkg.contains(".model") || pkg.contains(".service"))
                .check(SourceSet.ofSources(Map.of("Shelf.java", SHELF, "Sale.java", SALE, "User.java", user), null));
    }

    private static final String EXHAUSTIVE = """
                String label(Shelf shelf) {
                    return switch (shelf) {
                        case Perishable p -> "a";
                        case NonExpiring n -> "b";
                    };
                }
            """;

    @Test
    void flagsACastToAnAlternativeAndAnIgnoredSealedResultWithTheirLines() {
        String user = """
                package demo.service;

                class SellProduct {
                    void sell(Product product) {
                        product.canSell("today");
                        Sellable ok = (Sellable) product.canSell("today");
                    }
                """ + EXHAUSTIVE + "}\n";

        List<Violation> violations = checkSale(user);

        assertThat(violations).anySatisfy(v -> {
            assertThat(v.line()).isEqualTo(5);
            assertThat(v.message()).contains("The result of 'canSell' is a sealed type and is ignored");
        });
        assertThat(violations).anySatisfy(v -> {
            assertThat(v.line()).isEqualTo(6);
            assertThat(v.message()).contains("Cast to 'Sellable'");
        });
    }

    @Test
    void acceptsASealedResultConsumedByASwitch() {
        String user = """
                package demo.service;

                class SellProduct {
                    String sell(Product product) {
                        return switch (product.canSell("today")) {
                            case Sellable s -> "sold";
                            case PastSellByDate p -> "refused since " + p.since();
                        };
                    }
                """ + EXHAUSTIVE + "}\n";

        assertThat(checkSale(user)).isEmpty();
    }

    @Test
    void flagsASwitchThatMissesAnAlternative() {
        String user = """
                package demo.model;

                class User {
                    String label(Shelf shelf) {
                        return switch (shelf) {
                            case Perishable p -> "sell by";
                            default -> "other";
                        };
                    }
                }
                """;

        assertThat(check(user)).anySatisfy(v -> assertThat(v.message()).contains("No exhaustive switch over an ADT"));
    }
}
