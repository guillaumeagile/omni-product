package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ValueObjectFactoryRuleTest {

    private static List<Violation> check(String... sources) {
        Map<String, String> files = java.util.stream.IntStream.range(0, sources.length).boxed()
                .collect(java.util.stream.Collectors.toMap(i -> "File" + i + ".java", i -> sources[i]));
        return new ValueObjectFactoryRule(pkg -> pkg.contains(".model"))
                .check(SourceSet.ofSources(files, null));
    }

    private static final String CREATION = """
            package demo.model;

            public sealed interface PerishableCreation permits Perishable, InvalidDates {
            }

            record InvalidDates(String reason) implements PerishableCreation {
            }
            """;

    @Test
    void acceptsAFactoryReturningASealedResultOfItselfOrAnErrorRecord() {
        String perishable = """
                package demo.model;

                public record Perishable(int sellBy, int useBy) implements PerishableCreation {
                    public static PerishableCreation create(int sellBy, int useBy) {
                        return sellBy <= useBy ? new Perishable(sellBy, useBy) : new InvalidDates("sellBy > useBy");
                    }
                }
                """;

        assertThat(check(CREATION, perishable)).isEmpty();
    }

    @Test
    void acceptsAGenericSealedResultParameterisedByTheValueObject() {
        String validated = """
                package demo.model;

                public sealed interface Validated<T> permits Valid, Invalid {
                }

                record Valid<T>(T value) implements Validated<T> {
                }

                record Invalid<T>(String reason) implements Validated<T> {
                }
                """;
        String price = """
                package demo.model;

                public record Price(double base) {
                    public static Validated<Price> fromRawValue(double base) {
                        return base < 0 ? new Invalid<>("negative") : new Valid<>(new Price(base));
                    }
                }
                """;

        assertThat(check(validated, price)).isEmpty();
    }

    @Test
    void flagsAValueObjectThatThrowsInsteadOfReturningAResult() {
        String price = """
                package demo.model;

                public record Price(double base) {
                    public Price {
                        if (base < 0) throw new IllegalArgumentException();
                    }
                    public static Price of(double base) { return new Price(base); }
                }
                """;

        assertThat(check(price)).anySatisfy(v -> assertThat(v.message()).contains("No value object has a static create/build"));
    }

    @Test
    void rejectsAnUnsealedResultAndAnErrorThatIsNotARecord() {
        String either = """
                package demo.model;

                import io.vavr.control.Either;

                public record Price(double base) {
                    public static Either<String, Price> create(double base) { return null; }
                }
                """;
        String classError = """
                package demo.model;

                public sealed interface PriceCreation permits Price, PriceError {
                }

                final class PriceError implements PriceCreation {
                }

                record Price(double base) implements PriceCreation {
                    public static PriceCreation create(double base) { return null; }
                }
                """;

        assertThat(check(either)).hasSize(1);
        assertThat(check(classError)).hasSize(1);
    }
}
