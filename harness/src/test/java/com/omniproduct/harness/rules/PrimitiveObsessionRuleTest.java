package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.UbiquitousLanguageMap;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PrimitiveObsessionRuleTest {

    private static List<Violation> check(String source) {
        Map<String, String> byPath = new LinkedHashMap<>();
        byPath.put("Product.java", source);
        return new PrimitiveObsessionRule(UbiquitousLanguageMap.load(), pkg -> pkg.contains(".model"))
                .check(SourceSet.ofSources(byPath, null));
    }

    @Test
    void flagsRawValuesCarryingADomainConcept() {
        List<Violation> violations = check("""
                package demo.model;

                public class Product {
                    private long price;
                    private String slug;
                    private int quantity;
                    public long getQuantity() { return quantity; }
                    public void restock(int quantity) { }
                }
                """);

        assertThat(violations).extracting(Violation::line).containsExactly(4, 5, 6, 7, 8);
        assertThat(violations.get(0).message()).contains("'price'", "(Pricing)", "is a raw long");
    }

    @Test
    void flagsRecordComponentsAndCollectionsOfRawValues() {
        List<Violation> violations = check("""
                package demo.model;

                import java.util.List;

                public record Label(String slug) { }

                class Shelf {
                    List<String> slugs;
                }
                """);

        assertThat(violations).extracting(Violation::message).anyMatch(message -> message.contains("'slug'"));
        assertThat(violations).extracting(Violation::message).anyMatch(message -> message.contains("'slugs'"));
    }

    @Test
    void acceptsDomainTypesWrappingTheValue() {
        assertThat(check("""
                package demo.model;

                import java.math.BigDecimal;

                public record Price(BigDecimal amount) { }

                class Product {
                    private Price price;
                    public Price getPrice() { return price; }
                }
                """)).isEmpty();
    }

    @Test
    void ignoresGenericNames() {
        assertThat(check("""
                package demo.model;

                public class Product {
                    private String name;
                    private String description;
                    private int count;
                }
                """)).isEmpty();
    }

    @Test
    void leavesIdentifiersToTheIdentifierRule() {
        assertThat(check("""
                package demo.model;

                public class Supplier {
                    private String supplierId;
                    private String siren;
                }
                """)).isEmpty();
    }

    @Test
    void ignoresNamesThatOnlyTheWeakLifecycleContextMatches() {
        assertThat(check("""
                package demo.model;

                public class Product {
                    private String productName;
                }
                """)).isEmpty();
    }

    @Test
    void ignoresTypesOutsideTheModelPackage() {
        Map<String, String> byPath = new LinkedHashMap<>();
        byPath.put("Price.java", """
                package demo.service;

                public class Pricing {
                    private long price;
                }
                """);
        assertThat(new PrimitiveObsessionRule(UbiquitousLanguageMap.load(), pkg -> pkg.contains(".model"))
                .check(SourceSet.ofSources(byPath, null))).isEmpty();
    }
}
