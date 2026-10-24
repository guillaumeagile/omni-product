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
