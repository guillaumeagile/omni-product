package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionsArePipelinesRuleTest {

    private static List<Violation> check(String body) {
        String service = """
                package demo.service;

                class CatalogService {
                %s
                }
                """.formatted(body);
        return new CollectionsArePipelinesRule(pkg -> pkg.contains(".service"))
                .check(SourceSet.ofSources(Map.of("CatalogService.java", service), null));
    }

    // --- Ce que la règle attrape ---

    @Test
    void flagsAForEachLoopThatFillsAListWithTheLoopLine() {
        List<Violation> violations = check("""
                    List<String> labels(List<Product> products) {
                        List<String> labels = new ArrayList<>();
                        for (Product product : products) {
                            labels.add(product.getName());
                        }
                        return labels;
                    }
                """);

        assertThat(violations).singleElement().satisfies(v -> {
            assertThat(v.line()).isEqualTo(6);
            assertThat(v.message()).contains("'labels'", "'labels'", "Stream");
        });
    }

    // Esquives : une autre boucle, un autre verbe, un autre accumulateur.
    @ParameterizedTest
    @ValueSource(strings = {
            "for (int i = 0; i < products.size(); i++) { result.add(products.get(i)); }",
            "int i = 0; while (i < products.size()) { result.addAll(List.of(products.get(i))); i++; }",
            "Iterator<Product> it = products.iterator(); do { result.add(it.next()); } while (it.hasNext());",
            "for (Product p : products) { byName.put(p.name(), p); }",
            "for (Product p : products) { builder.append(p.name()); }",
            "for (Product p : products) { total += p.price(); }",
            "for (Product p : products) { count++; }",
            "for (Product p : products) { this.cache.add(p); }",
            "for (Product p : products) { CACHE.add(p); }",
            "for (Product p : products) { Collections.addAll(result, p); }",
            "for (Product p : products) { names[n] = p.name(); }",
            "products.forEach(p -> result.add(p));",
            "products.forEach(result::add);",
            "products.stream().filter(Product::isActive).forEach(p -> { total = total + 1; });",
            "IntStream.range(0, products.size()).forEach(i -> result.add(products.get(i)));",
            "products.stream().peek(result::add).count();"})
    void flagsEveryWayOfFillingAnAccumulator(String loop) {
        assertThat(check("""
                    Object collect(List<Product> products) {
                        %s
                        return null;
                    }
                """.formatted(loop))).hasSize(1);
    }

    // --- Ce que la règle accepte ---

    @Test
    void acceptsAStreamPipeline() {
        assertThat(check("""
                    List<String> labels(List<Product> products, LocalDate today) {
                        return products.stream()
                                .filter(product -> product.canSell(today))
                                .map(product -> switch (product.shelfLife()) {
                                    case Perishable p -> product.name();
                                    case NonExpiring n -> product.name() + " [BEST BEFORE PASSED]";
                                })
                                .toList();
                    }

                    Map<Boolean, List<Product>> split(List<Product> products) {
                        return products.stream().collect(Collectors.partitioningBy(Product::isPerishable));
                    }
                """)).isEmpty();
    }

    @Test
    void acceptsLoopsThatOnlyWorkOnTheirOwnVariables() {
        assertThat(check("""
                    void print(List<Product> products) {
                        for (Product product : products) {
                            String label = product.name();
                            label = label.trim();
                            System.out.println(label);
                        }
                        products.forEach(product -> {
                            List<String> words = new ArrayList<>();
                            words.add(product.name());
                            log(words);
                        });
                    }
                """)).isEmpty();
    }

    @Test
    void acceptsAForEachThatOnlyReadsOrDelegates() {
        assertThat(check("""
                    void notify(List<Product> products) {
                        products.forEach(this::publish);
                        products.forEach(product -> repository.findById(product.id()));
                    }
                """)).isEmpty();
    }
}
