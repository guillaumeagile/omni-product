package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessSeverity;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionComposesRuleTest {

    private static List<Violation> check(String body) {
        String service = """
                package demo.service;

                class SaleService {
                %s
                }
                """.formatted(body);
        return new DecisionComposesRule(pkg -> pkg.contains(".service"))
                .check(SourceSet.ofSources(Map.of("SaleService.java", service), null));
    }

    // --- Fixture mauvaise : la garde du départ de la phase 3 (ProductService.sellOne) ---

    @Test
    void flagsTheGuardClauseOfTheStartingCodeWithItsLine() {
        List<Violation> violations = check("""
                    Option<Stock> sellOne(Product product, Stock stock, LocalDate today) {
                        if (!product.canSellOn(today)) {
                            return Option.none();
                        }
                        return Option.of(withdraw(stock));
                    }
                """);

        assertThat(violations).singleElement().satisfies(v -> {
            assertThat(v.line()).isEqualTo(5);
            assertThat(v.message()).contains("'sellOne'", "Option.when");
        });
    }

    // --- Fixture bonne : la décision est une étape de la chaîne ---

    @ParameterizedTest
    @ValueSource(strings = {
            "return Option.when(product.canSellOn(today), () -> withdraw(stock));",
            "return Option.of(stock).filter(s -> product.canSellOn(today)).map(this::withdraw);",
            "return Option.of(product).filter(p -> p.canSellOn(today)).map(p -> withdraw(stock));",
            "return product.sellability(today).fold(refusal -> Option.none(), ok -> Option.of(withdraw(stock)));"})
    void acceptsADecisionComposedAsAStep(String statement) {
        assertThat(check("""
                    Option<Stock> sellOne(Product product, Stock stock, LocalDate today) {
                        %s
                    }
                """.formatted(statement))).isEmpty();
    }

    @Test
    void ignoresAnIfOnSomethingThatIsNotADecision() {
        assertThat(check("""
                    Option<Stock> sellOne(Stock stock) {
                        if (stock.isEmpty()) {
                            return Option.none();
                        }
                        return Option.of(stock);
                    }
                """)).isEmpty();
    }

    @Test
    void ignoresADecisionIfWhoseBranchDoesNotLeaveTheMethod() {
        assertThat(check("""
                    void refresh(Product product, Counter counter) {
                        if (product.canSellOn(today)) {
                            counter.reset();
                        }
                    }
                """)).isEmpty();
    }

    @Test
    void ignoresAReturnThatBelongsToALambdaInTheBranch() {
        assertThat(check("""
                    void notify(Product product, List<Listener> listeners) {
                        if (product.canSellOn(today)) {
                            listeners.forEach(listener -> { listener.sold(); return; });
                        }
                    }
                """)).isEmpty();
    }

    // --- Esquives : renommer, inverser, recopier la décision, changer de forme ---

    @ParameterizedTest
    @ValueSource(strings = {
            "if (product.canSellOn(today)) { return Option.of(withdraw(stock)); } return Option.none();",
            "if (product.CAN_SELL(today)) { return Option.of(withdraw(stock)); } return Option.none();",
            "if (product.can_sell_on(today)) { return Option.of(withdraw(stock)); } return Option.none();",
            "if (product.canPurchase(today)) { return Option.of(withdraw(stock)); } return Option.none();",
            "if (product.isSellable(today)) { return Option.of(withdraw(stock)); } return Option.none();",
            "if (product.isSaleAllowed(today)) { return Option.of(withdraw(stock)); } return Option.none();",
            "if (!(product.canSellOn(today) && stock.hasRoom())) return Option.none(); return Option.of(stock);",
            "if (stock.hasRoom() || product.canSellOn(today)) return Option.of(stock); return Option.none();",
            "if (product.canSellOn(today)) return Option.of(stock); else return Option.none();",
            "if (product.canSellOn(today)) { log(); } else { throw new IllegalStateException(); } return Option.of(stock);",
            "boolean ok = product.canSellOn(today); if (!ok) { return Option.none(); } return Option.of(stock);",
            "boolean ok = product.canSellOn(today); boolean again = !ok; if (again) { return Option.none(); } return Option.of(stock);",
            "return product.canSellOn(today) ? Option.of(withdraw(stock)) : Option.none();",
            "return !product.canSellOn(today) ? Option.none() : Option.of(withdraw(stock));",
            "if (Optional.of(product).map(Product::canSellOn).isPresent()) { return Option.of(stock); } return Option.none();"})
    void flagsEveryWayOfTestingTheDecision(String statements) {
        assertThat(check("""
                    Option<Stock> sellOne(Product product, Stock stock, LocalDate today) {
                        %s
                    }
                """.formatted(statements))).hasSize(1);
    }

    @Test
    void flagsAPrivateMethodAndAConstructorToo() {
        assertThat(check("""
                    private Option<Stock> guard(Product product, Stock stock) {
                        if (!product.canSellOn(today)) { return Option.none(); }
                        return Option.of(stock);
                    }
                """)).hasSize(1);
    }

    @Test
    void isAWarningThatNeverFailsTheHarness() {
        assertThat(new DecisionComposesRule(pkg -> true).severity()).isEqualTo(HarnessSeverity.WARNING);
        assertThat(new DecisionComposesRule(pkg -> true).id()).isEqualTo("HA16");
    }

    @Test
    void staysOutOfOtherPackages() {
        String model = """
                package demo.model;

                class Product {
                    Option<Stock> sell(Stock stock) {
                        if (!canSellOn(today)) { return Option.none(); }
                        return Option.of(stock);
                    }
                }
                """;
        assertThat(new DecisionComposesRule(pkg -> pkg.contains(".service"))
                .check(SourceSet.ofSources(Map.of("Product.java", model), null))).isEmpty();
    }
}
