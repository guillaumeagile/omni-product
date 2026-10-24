package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ValueObjectFactoryRuleTest {

    private static final String ERROR = """
            package demo.model;

            public record PriceError(String reason) {
            }
            """;

    private static List<Violation> check(String price) {
        return new ValueObjectFactoryRule(pkg -> pkg.contains(".model"))
                .check(SourceSet.ofSources(Map.of("Price.java", price, "PriceError.java", ERROR), null));
    }

    @Test
    void acceptsAFactoryReturningEitherOfTheValueObjectOrADeclaredError() {
        String price = """
                package demo.model;

                import io.vavr.control.Either;

                public record Price(double base) {
                    public static Either<PriceError, Price> create(double base) {
                        return base < 0 ? Either.left(new PriceError("negative")) : Either.right(new Price(base));
                    }
                }
                """;

        assertThat(check(price)).isEmpty();
    }

    @Test
    void flagsAValueObjectWithoutAValidatingFactory() {
        String price = """
                package demo.model;

                public record Price(double base) {
                    public Price {
                        if (base < 0) throw new IllegalArgumentException();
                    }
                }
                """;

        assertThat(check(price)).anySatisfy(v -> assertThat(v.message()).contains("No value object has a static create/build"));
    }

    @Test
    void rejectsAnErrorTypeThatIsNotDeclaredInTheCode() {
        String price = """
                package demo.model;

                import io.vavr.control.Either;

                public record Price(double base) {
                    public static Either<String, Price> create(double base) {
                        return null;
                    }
                }
                """;

        assertThat(check(price)).anySatisfy(v -> assertThat(v.message()).contains("No value object has a static create/build"));
    }

    @Test
    void evasionOfTheFactoryNameIsStillCaught() {
        String price = """
                package demo.model;

                import io.vavr.control.Either;

                public record Price(double base) {
                    public static Either<PriceError, Price> fromRawValue(double base) {
                        return null;
                    }
                }
                """;

        assertThat(check(price)).isEmpty();
    }
}
