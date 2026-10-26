package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TellDontAskRuleTest {

    private static final String MODEL = """
            package demo.model;

            public record ProductStock(Quantity stock, Quantity quantity, String warehouse) {}
            """;

    private static List<Violation> check(String body) {
        String service = """
                package demo.service;

                class StockService {
                %s
                }
                """.formatted(body);
        return new TellDontAskRule(pkg -> pkg.contains(".service"))
                .check(SourceSet.ofSources(Map.of("ProductStock.java", MODEL, "StockService.java", service), null));
    }

    // --- Fixture mauvaise : la méthode du départ de la phase 3 (ProductService.withdrawStock) ---

    @Test
    void flagsTheRebuildOfTheStartingCodeWithItsLine() {
        List<Violation> violations = check("""
                    private ProductStock withdrawStock(ProductStock stock) {
                        return new ProductStock(new Quantity(stock.stock().value() - 1), stock.quantity(), stock.warehouse());
                    }
                """);

        assertThat(violations).singleElement().satisfies(v -> {
            assertThat(v.filePath()).isEqualTo("StockService.java");
            assertThat(v.line()).isEqualTo(5);
            assertThat(v.message()).contains("'withdrawStock'", "ProductStock", "3 accessors", "'stock'");
        });
    }

    // --- Fixtures bonnes : on dit à l'objet quoi faire, ou on ne le reconstruit pas depuis lui-même ---

    @ParameterizedTest
    @ValueSource(strings = {
            "return stock.withdrawOne();",
            "return new ProductStock(new Quantity(0), stock.quantity(), \"main\");",
            "return new ProductStock(new Quantity(0), new Quantity(1), \"main\");",
            "return new Quantity(stock.stock().value() - 1);",
            "return new ProductStock(stock.stock(), stock.stock(), stock.stock().toString());"})
    void acceptsTellingTheObjectOrReadingASingleAccessor(String statement) {
        assertThat(check("""
                    Object withdraw(ProductStock stock) {
                        %s
                    }
                """.formatted(statement))).isEmpty();
    }

    @Test
    void ignoresTwoDifferentParametersMixedTogether() {
        assertThat(check("""
                    ProductStock merge(ProductStock first, ProductStock second) {
                        return new ProductStock(first.stock(), second.quantity(), "main");
                    }
                """)).isEmpty();
    }

    @Test
    void ignoresAParameterOfALibraryType() {
        assertThat(check("""
                    Optional<String> copy(Optional<String> source) {
                        return Optional.of(source.get() + source.orElse(""));
                    }
                """)).isEmpty();
    }

    @Test
    void staysOutOfOtherPackages() {
        String model = """
                package demo.model;

                public record Other(ProductStock stock) {
                    ProductStock again(ProductStock s) { return new ProductStock(s.stock(), s.quantity(), s.warehouse()); }
                }
                """;
        assertThat(new TellDontAskRule(pkg -> pkg.contains(".service"))
                .check(SourceSet.ofSources(Map.of("ProductStock.java", MODEL, "Other.java", model), null))).isEmpty();
    }

    // --- Esquives : renommer l'accesseur, passer par une variable, une fabrique, une aide, un autre moule ---

    @ParameterizedTest
    @ValueSource(strings = {
            "return new ProductStock(stock.getStock(), stock.getQuantity(), stock.getWarehouse());",
            "return new ProductStock(stock.fetchStock(), stock.loadQuantity(), \"main\");",
            "return new ProductStock(stock.stock, stock.quantity, \"main\");",
            "return new ProductStock(stock.stock(), stock.quantity(), \"main\");",
            "return new demo.model.ProductStock(stock.stock(), stock.quantity(), \"main\");",
            "return ProductStock.of(stock.stock(), stock.quantity(), \"main\");",
            "return ProductStock.rebuild(stock.stock(), stock.quantity(), \"main\");",
            "var q = stock.quantity(); return new ProductStock(stock.stock(), q, \"main\");",
            "var q = stock.quantity(); var w = q; return new ProductStock(new Quantity(0), w, stock.warehouse());",
            "Quantity q = stock.quantity(); Quantity s = stock.stock(); return new ProductStock(s, q, \"main\");",
            "return Option.of(1).map(i -> new ProductStock(stock.stock(), stock.quantity(), \"main\")).get();",
            "return stock == null ? null : new ProductStock(stock.stock(), stock.quantity(), \"main\");",
            "return new ProductStock(new Quantity(stock.stock().value() - 1), new Quantity(stock.quantity().value()), \"main\");"})
    void flagsEveryWayOfRebuildingFieldByField(String statement) {
        assertThat(check("""
                    Object withdraw(ProductStock stock) {
                        %s
                    }
                """.formatted(statement))).hasSize(1);
    }

    @Test
    void flagsAConstructorAndAPublicMethodToo() {
        assertThat(check("""
                    public ProductStock once(ProductStock stock) {
                        return new ProductStock(stock.stock(), stock.quantity(), "x");
                    }
                    private ProductStock twice(ProductStock stock) {
                        return new ProductStock(stock.stock(), stock.warehouse() == null ? stock.quantity() : stock.quantity(), "x");
                    }
                """)).hasSize(2);
    }

    @Test
    void isAnErrorThatFailsTheHarness() {
        assertThat(new TellDontAskRule(pkg -> true).severity().name()).isEqualTo("ERROR");
        assertThat(new TellDontAskRule(pkg -> true).id()).isEqualTo("HA17");
    }
}
