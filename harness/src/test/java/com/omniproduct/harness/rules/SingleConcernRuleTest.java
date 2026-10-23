package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.UbiquitousLanguageMap;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SingleConcernRuleTest {

    private static List<Violation> check(String source) {
        return new SingleConcernRule(UbiquitousLanguageMap.load(), 1, pkg -> pkg.contains(".model") || pkg.contains(".service"))
                .check(SourceSet.ofSources(Map.of("Thing.java", source), null));
    }

    @Test
    void flagsATypeMixingSeveralBoundedContexts() {
        List<Violation> violations = check("""
                package demo.service;

                public class ShopService {
                    public void updateSlug(String s) {}
                    public double getPrice() { return 0; }
                    public int getStock() { return 0; }
                }
                """);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).line()).isEqualTo(3);
        assertThat(violations.get(0).message()).contains("'ShopService' mixes 3 concerns", "Catalog [updateSlug]", "Pricing [getPrice]", "Storage [getStock]");
    }

    @Test
    void acceptsATypeWithASingleConcernAndIgnoresGenericNames() {
        assertThat(check("""
                package demo.model;

                public class Price {
                    public String getId() { return null; }
                    public Double getBase() { return null; }
                    public Double getTax() { return null; }
                    public Double getTaxRate() { return null; }
                }
                """)).isEmpty();
    }

    @Test
    void flagsVocabularyThatWasExtractedOutOfAType() {
        List<Violation> violations = check("""
                package demo.model;

                public class Product {
                    public String getSlug() { return null; }
                }
                """);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).line()).isEqualTo(4);
        assertThat(violations.get(0).message()).contains("'Product.getSlug'", "'Slug'");
    }

    @Test
    void readsRecordComponentsAsMembers() {
        assertThat(check("package demo.model;\npublic record Mixed(double price, int stock) {}\n")).hasSize(1);
    }

    @Test
    void readsWordsAnywhereInTheName() {
        var map = UbiquitousLanguageMap.load();
        assertThat(map.concernOf("getProductSlug")).contains("Catalog");
        assertThat(map.concernOf("fetch_slug")).contains("Catalog");
        assertThat(map.concernOf("SLUGS")).contains("Catalog");
        assertThat(map.concernOf("renderDisplayLabel")).contains("Catalog");
        assertThat(map.concernOf("getAllSuppliers")).contains("Supplier");
        assertThat(map.concernOf("getProductName")).contains("Lifecycle");
        assertThat(map.concernOf("findById")).isEmpty();
        assertThat(map.concernOf("settings")).isEmpty();
    }

    @Test
    void doesNotNeedTheLeadingVerbToBeOneOfTheKnownOnes() {
        assertThat(check("""
                package demo.service;

                public class ShopService {
                    public void retrieveSlug() {}
                    public double obtainPrice() { return 0; }
                }
                """)).hasSize(1);
    }

    @Test
    void readsHiddenApiVisibilityAndFields() {
        assertThat(check("""
                package demo.model;

                public class Sneaky {
                    public double margin;
                    int stock() { return 0; }
                    protected String slug() { return null; }
                }
                """)).hasSize(1);
    }

    @Test
    void fallsBackOnTheTypesAMemberHandlesWhenItsNameIsGeneric() {
        assertThat(check("""
                package demo.service;

                public class ShopService {
                    public Price compute() { return null; }
                    public void apply(Stock stock) {}
                }
                """)).hasSize(1);
        assertThat(check("""
                package demo.model;

                public class Product {
                    public Slug label() { return null; }
                }
                """)).hasSize(1);
    }
}
