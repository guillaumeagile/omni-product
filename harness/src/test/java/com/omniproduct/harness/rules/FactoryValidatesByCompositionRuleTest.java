package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FactoryValidatesByCompositionRuleTest {

    private static List<Violation> check(String... sources) {
        Map<String, String> files = java.util.stream.IntStream.range(0, sources.length).boxed()
                .collect(java.util.stream.Collectors.toMap(i -> "File" + i + ".java", i -> sources[i]));
        return new FactoryValidatesByCompositionRule(pkg -> pkg.contains(".model"))
                .check(SourceSet.ofSources(files, null));
    }

    // --- Bons cas : la validation est une chaîne d'étapes, chacune une valeur ---

    @Test
    void acceptsAFactoryComposingOptionSteps() {
        String perishable = """
                package demo.model;

                import io.vavr.control.Option;
                import java.time.LocalDate;

                public record Perishable(LocalDate sellBy, LocalDate useBy) {
                    public static Option<Perishable> of(LocalDate sellBy, LocalDate useBy) {
                        return Option.of(sellBy)
                                .flatMap(s -> Option.of(useBy).filter(u -> !u.isBefore(s)).map(u -> new Perishable(s, u)));
                    }
                }
                """;

        assertThat(check(perishable)).isEmpty();
    }

    @Test
    void acceptsAFactoryCombiningValidationsWithAMethodReference() {
        String perishable = """
                package demo.model;

                import io.vavr.control.Validation;
                import io.vavr.control.Option;
                import java.time.LocalDate;

                public record Perishable(LocalDate sellBy, LocalDate useBy) {
                    public static Validation<String, Perishable> create(LocalDate sellBy, LocalDate useBy) {
                        return Validation.combine(present(sellBy, "sellBy"), present(useBy, "useBy")).ap(Perishable::new);
                    }

                    private static Validation<String, LocalDate> present(LocalDate date, String name) {
                        return Option.of(date).toValidation(() -> name + " is missing");
                    }
                }
                """;

        assertThat(check(perishable)).isEmpty();
    }

    @Test
    void acceptsAFactoryUsingOptionWhenWithASupplier() {
        String price = """
                package demo.model;

                import io.vavr.control.Option;

                public record Price(double base) {
                    public static Option<Price> fromRawValue(double base) {
                        return Option.when(base >= 0, () -> new Price(base));
                    }
                }
                """;

        assertThat(check(price)).isEmpty();
    }

    // --- Rouge par absence : rien à examiner ne veut pas dire que c'est bon ---

    private static void assertOnlyAbsence(List<Violation> violations) {
        assertThat(violations).singleElement().satisfies(v -> {
            assertThat(v.line()).isEqualTo(1);
            assertThat(v.message()).contains("No value object has a static factory whose result can fail");
        });
    }

    @Test
    void isRedWhenThereIsNoFactoryAtAll() {
        String price = """
                package demo.model;

                public record Price(double base) {
                }
                """;

        assertOnlyAbsence(check(price));
    }

    @Test
    void isRedWhenTheModelIsEmpty() {
        assertOnlyAbsence(check());
    }

    @Test
    void aTrivialFactoryReturningThePlainRecordDoesNotMakeItGreen() {
        String slug = """
                package demo.model;

                public record Slug(String value) {
                    public static Slug of(String raw) { return new Slug(raw.toLowerCase()); }
                }
                """;

        assertOnlyAbsence(check(slug));
    }

    @Test
    void aFactoryThatCanFailMakesItGreenEvenNextToATrivialOne() {
        String slug = """
                package demo.model;

                public record Slug(String value) {
                    public static Slug of(String raw) { return new Slug(raw.toLowerCase()); }
                }
                """;
        String price = """
                package demo.model;

                import io.vavr.control.Option;

                public record Price(double base) {
                    public static Option<Price> of(double base) { return Option.when(base >= 0, () -> new Price(base)); }
                }
                """;

        assertThat(check(slug, price)).isEmpty();
    }

    @Test
    void ignoresEntitiesAndInstanceMethods() {
        String entity = """
                package demo.model;

                import io.vavr.control.Option;

                public class Product implements Identifiable {
                    public static Option<Product> of(String name) {
                        if (name == null) return Option.none();
                        return Option.of(new Product());
                    }
                }
                """;
        String instance = """
                package demo.model;

                import io.vavr.control.Option;

                public record Price(double base) {
                    public Option<Price> discounted(double rate) {
                        if (rate < 0) return Option.none();
                        return Option.of(new Price(base * (1 - rate)));
                    }
                }
                """;

        // Ni l'entité (pas un value object) ni la méthode d'instance (pas une fabrique) ne sont jugées, ni ne comptent :
        // seule l'absence est signalée, jamais leur `if`.
        assertOnlyAbsence(check(entity));
        assertOnlyAbsence(check(instance));
    }

    @Test
    void ignoresFilesOutsideTheModel() {
        String service = """
                package demo.service;

                import io.vavr.control.Option;

                public record Price(double base) {
                    public static Option<Price> of(double base) {
                        if (base < 0) return Option.none();
                        return Option.of(new Price(base));
                    }
                }
                """;

        // Le `if` du service n'est pas jugé (hors modèle) : il ne reste que l'absence de fabrique dans le modèle.
        assertOnlyAbsence(check(service));
    }

    // --- Mauvais cas : la fabrique branche, lance, ou ne décide rien ---

    @Test
    void flagsAFactoryThatBranchesWithIf() {
        String perishable = """
                package demo.model;

                import io.vavr.control.Option;
                import java.time.LocalDate;

                public record Perishable(LocalDate sellBy, LocalDate useBy) {
                    public static Option<Perishable> of(LocalDate sellBy, LocalDate useBy) {
                        if (sellBy == null || useBy == null || useBy.isBefore(sellBy)) {
                            return Option.none();
                        }
                        return Option.of(new Perishable(sellBy, useBy));
                    }
                }
                """;

        assertThat(check(perishable)).singleElement().satisfies(v -> {
            assertThat(v.line()).isEqualTo(8);
            assertThat(v.message()).contains("Factory 'of' of 'Perishable'", "branches with 'if'");
        });
    }

    @Test
    void flagsAFactoryThatThrowsOrCallsAGuard() {
        String throwing = """
                package demo.model;

                import io.vavr.control.Option;

                public record Price(double base) {
                    public static Option<Price> of(double base) {
                        throw new IllegalArgumentException();
                    }
                }
                """;
        String guarded = """
                package demo.model;

                import io.vavr.control.Option;
                import java.util.Objects;

                public record Price(Object base) {
                    public static Option<Price> of(Object base) {
                        return Option.of(new Price(Objects.requireNonNull(base)));
                    }
                }
                """;

        assertThat(check(throwing)).singleElement().satisfies(v -> assertThat(v.message()).contains("contains 'throw'"));
        assertThat(check(guarded)).singleElement().satisfies(v -> assertThat(v.message()).contains("calls the guard 'requireNonNull'"));
    }

    @Test
    void flagsAFactoryThatPromisesAFailureButNeverDecides() {
        String option = """
                package demo.model;

                import io.vavr.control.Option;

                public record Price(double base) {
                    public static Option<Price> of(double base) { return Option.of(new Price(base)); }
                }
                """;
        String sealedResult = """
                package demo.model;

                public sealed interface PriceCreation permits Price, InvalidPrice {
                }

                record InvalidPrice(String reason) implements PriceCreation {
                }

                record Price(double base) implements PriceCreation {
                    public static PriceCreation create(double base) { return new Price(base); }
                }
                """;

        assertThat(check(option)).singleElement().satisfies(v -> {
            assertThat(v.line()).isEqualTo(6);
            assertThat(v.message()).contains("never decides anything");
        });
        assertThat(check(sealedResult)).singleElement().satisfies(v -> assertThat(v.message()).contains("never decides anything"));
    }

    // --- Évasions : le défaut déplacé ou renommé est toujours vu ---

    @Test
    void stillSeesATernaryOrASwitchInsteadOfAnIf() {
        String ternary = """
                package demo.model;

                import io.vavr.control.Option;

                public record Price(double base) {
                    public static Option<Price> of(double base) {
                        return base < 0 ? Option.none() : Option.of(new Price(base));
                    }
                }
                """;
        String switched = """
                package demo.model;

                import io.vavr.control.Option;

                public record Price(double base) {
                    public static Option<Price> of(double base) {
                        return switch (Boolean.valueOf(base < 0)) {
                            case Boolean b when b -> Option.none();
                            default -> Option.of(new Price(base));
                        };
                    }
                }
                """;

        assertThat(check(ternary)).singleElement().satisfies(v -> assertThat(v.message()).contains("branches with '?:'"));
        assertThat(check(switched)).singleElement().satisfies(v -> assertThat(v.message()).contains("branches with 'switch'"));
    }

    @Test
    void stillSeesAnIfExtractedIntoAPrivateHelperOfTheSameType() {
        String extracted = """
                package demo.model;

                import io.vavr.control.Option;
                import java.time.LocalDate;

                public record Perishable(LocalDate sellBy, LocalDate useBy) {
                    public static Option<Perishable> of(LocalDate sellBy, LocalDate useBy) {
                        return validated(sellBy, useBy);
                    }

                    private static Option<Perishable> validated(LocalDate sellBy, LocalDate useBy) {
                        return ordered(sellBy, useBy);
                    }

                    private static Option<Perishable> ordered(LocalDate sellBy, LocalDate useBy) {
                        if (useBy.isBefore(sellBy)) {
                            return Option.none();
                        }
                        return Option.of(new Perishable(sellBy, useBy));
                    }
                }
                """;

        assertThat(check(extracted)).singleElement().satisfies(v -> {
            assertThat(v.line()).isEqualTo(16);
            assertThat(v.message()).contains("Factory 'of' of 'Perishable'", "branches with 'if'");
        });
    }

    @Test
    void stillSeesAnIfHiddenInsideALambda() {
        String inLambda = """
                package demo.model;

                import io.vavr.control.Option;

                public record Price(double base) {
                    public static Option<Price> of(double base) {
                        return Option.of(base).flatMap(b -> {
                            if (b < 0) return Option.none();
                            return Option.of(new Price(b));
                        });
                    }
                }
                """;

        assertThat(check(inLambda)).singleElement().satisfies(v -> assertThat(v.message()).contains("branches with 'if'"));
    }

    @Test
    void stillSeesAFactoryRenamedAwayFromTheFactoryWordsWhenItReturnsTheRecord() {
        String renamed = """
                package demo.model;

                import io.vavr.control.Either;

                public record Price(double base) {
                    public static Either<String, Price> parse(double base) {
                        if (base < 0) return Either.left("negative");
                        return Either.right(new Price(base));
                    }
                }
                """;
        String qualifiedGuard = """
                package demo.model;

                import io.vavr.control.Option;

                public record Price(Object base) {
                    public static Option<Price> fromRawValue(Object base) {
                        java.util.Objects.requireNonNull(base);
                        return Option.of(new Price(base));
                    }
                }
                """;

        assertThat(check(renamed)).singleElement().satisfies(v -> assertThat(v.message()).contains("Factory 'parse' of 'Price'"));
        assertThat(check(qualifiedGuard)).singleElement().satisfies(v -> assertThat(v.message()).contains("calls the guard 'requireNonNull'"));
    }

    @Test
    void reportsEachSmellOnceEvenWhenAHelperIsCalledTwice() {
        String twice = """
                package demo.model;

                import io.vavr.control.Option;

                public record Price(double base, double tax) {
                    public static Option<Price> of(double base, double tax) {
                        return positive(base).flatMap(b -> positive(tax).map(t -> new Price(b, t)));
                    }

                    private static Option<Double> positive(double value) {
                        if (value < 0) return Option.none();
                        return Option.of(value);
                    }
                }
                """;

        assertThat(check(twice)).hasSize(1);
    }
}
