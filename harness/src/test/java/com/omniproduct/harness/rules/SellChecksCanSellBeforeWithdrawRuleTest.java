package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SellChecksCanSellBeforeWithdrawRuleTest {

    private static List<Violation> check(String body) {
        String service = """
                package demo.service;

                class SellService {
                %s
                }
                """.formatted(body);
        return new SellChecksCanSellBeforeWithdrawRule(pkg -> pkg.contains(".service"))
                .check(SourceSet.ofSources(Map.of("SellService.java", service), null));
    }

    // --- Ce que la règle attrape ---

    @Test
    void flagsTheLegacySaleThatNeverAsksAndPointsAtTheWithdrawal() {
        List<Violation> violations = check("""
                    public ProductStock sellOne(ProductStock stock) {
                        return withdrawStock(stock);
                    }

                    private ProductStock withdrawStock(ProductStock stock) {
                        return new ProductStock(new Quantity(stock.stock().value() - 1), stock.quantity());
                    }
                """);

        assertThat(violations).singleElement().satisfies(v -> {
            assertThat(v.line()).isEqualTo(5);
            assertThat(v.message()).contains("'sellOne'", "without ever asking");
        });
    }

    @Test
    void flagsACanSellWhoseAnswerIsIgnoredEvenWhenItComesFirst() {
        List<Violation> violations = check("""
                    void sell(Product product, ProductStock stock, LocalDate today) {
                        product.canSell(today);
                        stock.withdraw(1);
                    }
                """);

        assertThat(violations).singleElement()
                .satisfies(v -> assertThat(v.message()).contains("does not depend on its answer"));
    }

    @Test
    void flagsAWithdrawalOutsideTheBranchThatChecks() {
        assertThat(check("""
                    void sell(Product product, ProductStock stock, LocalDate today) {
                        if (product.canSell(today)) {
                            log("ok");
                        }
                        stock.withdraw(1);
                    }
                """)).hasSize(1);
    }

    @Test
    void flagsAnEagerArgumentEvenNextToTheDecision() {
        assertThat(check("""
                    Option<ProductStock> sell(Product product, ProductStock stock, LocalDate today) {
                        return Option.when(product.canSell(today), stock.withdraw(1));
                    }
                """)).hasSize(1);
    }

    // Esquives : renommer la méthode, le retrait, ou retirer à la main.
    @ParameterizedTest
    @ValueSource(strings = {"dispatchOne", "sellProducts", "SELL_ONE", "purchase", "checkout"})
    void flagsWhateverTheSaleMethodIsCalled(String name) {
        assertThat(check("""
                    public ProductStock %s(ProductStock stock) {
                        return stock.decrementStock();
                    }
                """.formatted(name))).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "stock.withdrawOne()",
            "stock.deductQuantity(1)",
            "new ProductStock(stock.stock().minus(1), stock.quantity())",
            "new ProductStock(stock.count() - 1, stock.quantity())",
            "stock.withStock(stock.stock().subtract(one))"})
    void flagsEveryWayOfTakingStock(String withdrawal) {
        assertThat(check("""
                    ProductStock sell(ProductStock stock) {
                        return %s;
                    }
                """.formatted(withdrawal))).hasSize(1);
    }

    @Test
    void followsHelpersTransitively() {
        assertThat(check("""
                    public ProductStock sell(ProductStock stock) {
                        return this.prepare(stock);
                    }

                    private ProductStock prepare(ProductStock stock) {
                        return take(stock);
                    }

                    private ProductStock take(ProductStock stock) {
                        return stock.withdraw(1);
                    }
                """)).singleElement().satisfies(v -> assertThat(v.message()).contains("'sell'"));
    }

    // --- Ce que la règle accepte ---

    @Test
    void acceptsAWithdrawalInsideTheBranchThatChecks() {
        assertThat(check("""
                    void sell(Product product, ProductStock stock, LocalDate today) {
                        if (product.canSell(today)) {
                            stock.withdraw(1);
                        }
                    }
                """)).isEmpty();
    }

    @Test
    void acceptsASwitchOnTheDecisionCarriedByAVariable() {
        assertThat(check("""
                    SaleResult sell(Product product, ProductStock stock, LocalDate today) {
                        SaleDecision decision = product.canSell(today);
                        return switch (decision) {
                            case Sellable s -> new Sold(withdrawStock(stock));
                            case PastSellByDate p -> new Refused(p);
                        };
                    }

                    private ProductStock withdrawStock(ProductStock stock) {
                        return new ProductStock(stock.stock().minus(1), stock.quantity());
                    }
                """)).isEmpty();
    }

    @Test
    void acceptsAFunctionalPipelineOnTheDecision() {
        assertThat(check("""
                    Option<ProductStock> sellOne(Product product, ProductStock stock, LocalDate today) {
                        return product.checkCanSell(today).map(sellable -> stock.withdrawStock(1));
                    }

                    Option<ProductStock> sellAgain(Product product, ProductStock stock, LocalDate today) {
                        return Option.of(stock).filter(s -> product.isSellable(today)).map(this::withdrawStock);
                    }

                    Option<ProductStock> sellLazily(Product product, ProductStock stock, LocalDate today) {
                        return Option.when(product.canSell(today), () -> stock.withdraw(1));
                    }
                """)).isEmpty();
    }

    @Test
    void acceptsAGuardClauseThatReturnsTheRefusalFirst() {
        assertThat(check("""
                    Option<ProductStock> sell(Product product, ProductStock stock, LocalDate today) {
                        if (!product.canSell(today)) {
                            return Option.none();
                        }
                        return Option.some(stock.withdraw(1));
                    }
                """)).isEmpty();
    }

    @Test
    void ignoresStockOperationsThatAreNotSales() {
        assertThat(check("""
                    public ProductStock restock(ProductStock stock) {
                        return stock.withdraw(-5);
                    }

                    public ProductStock discardExpired(ProductStock stock) {
                        return stock.withdraw(1);
                    }

                    public ProductStock withdrawStock(ProductStock stock) {
                        return new ProductStock(stock.stock().minus(1), stock.quantity());
                    }
                """)).isEmpty();
    }

    @Test
    void ignoresASaleThatOnlyComputesAPrice() {
        assertThat(check("""
                    Amount salePrice(Amount base, Amount discount) {
                        return base.subtract(discount);
                    }
                """)).isEmpty();
    }
}
