package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecordsAreValueObjectsRuleTest {

    private static List<Violation> check(Map<String, String> sources) {
        return new RecordsAreValueObjectsRule(pkg -> pkg.contains(".model"))
                .check(SourceSet.ofSources(sources, null));
    }

    private static final String PRICE = """
            package demo.model.valueobject;

            public record Price(double base, double taxRate) {
            }
            """;

    @Test
    void acceptsRecordsAndAPriceValueObject() {
        assertThat(check(Map.of("Price.java", PRICE))).isEmpty();
    }

    @Test
    void flagsARecordThatImplementsIdentifiableWithItsLine() {
        String source = """
                package demo.model;

                public record Line(String id) implements Identifiable {
                }
                """;

        assertThat(check(Map.of("Line.java", source, "Price.java", PRICE)))
                .anySatisfy(v -> {
                    assertThat(v.line()).isEqualTo(3);
                    assertThat(v.message()).contains("'Line' is a record that implements Identifiable");
                });
    }

    @Test
    void flagsAClassInTheValueObjectPackage() {
        String source = """
                package demo.model.value_objects;

                public class Money {
                }
                """;

        assertThat(check(Map.of("Money.java", source, "Price.java", PRICE)))
                .anySatisfy(v -> assertThat(v.message()).contains("'Money' is a class in the value-object package"));
    }

    @Test
    void requiresAPriceRecordInTheValueObjectPackage() {
        String priceClass = """
                package demo.model;

                public class Price {
                }
                """;

        assertThat(check(Map.of("Price.java", priceClass)))
                .anySatisfy(v -> assertThat(v.message()).contains("No record named 'Price'"));
    }

    @Test
    void evasionsOfTheIdentifiableNameAreStillCaught() {
        String qualified = """
                package demo.model;

                public record Line(String name) implements com.acme.Identifiable<LineId> {
                }
                """;
        String generic = """
                package demo.model;

                public record Tag(String name) implements IdentifiableEntity {
                }
                """;

        assertThat(check(Map.of("Line.java", qualified, "Tag.java", generic, "Price.java", PRICE)))
                .anySatisfy(v -> assertThat(v.message()).contains("'Line' is a record that implements Identifiable"))
                .anySatisfy(v -> assertThat(v.message()).contains("'Tag' is a record that implements Identifiable"));
    }

    @Test
    void evasionOfThePackageNameIsStillCaught() {
        String source = """
                package demo.model.valueObjects;

                public class Money {
                }
                """;

        assertThat(check(Map.of("Money.java", source, "Price.java", PRICE)))
                .anySatisfy(v -> assertThat(v.message()).contains("'Money' is a class in the value-object package"));
    }
}
