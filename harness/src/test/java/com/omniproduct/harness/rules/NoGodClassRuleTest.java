package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NoGodClassRuleTest {

    private static final String GOD = """
            package demo.model;
            // Accentué : é à ü, pour vérifier les offsets UTF-8
            public class Product {
                public String getName() { return null; }
                public String getSlug() { return null; }
                public int getStock() { return 0; }
                public double getPrice() { return 0; }
                public boolean isActive() { return true; }
                public String getProductId() { return null; }
            }
            """;

    private static List<Violation> check(String path, String source) {
        return new NoGodClassRule(4, pkg -> pkg.contains(".model"))
                .check(SourceSet.ofSources(Map.of(path, source), null));
    }

    @Test
    void flagsClassWithTooManyPublicProperties() {
        List<Violation> violations = check("Product.java", GOD);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).line()).isEqualTo(3);
        assertThat(violations.get(0).message()).contains("'Product'", "5 public properties");
    }

    @Test
    void idPropertiesDoNotCount() {
        // 4 propriétés hors id + productId ignoré = pile à la limite : conforme.
        String atLimit = GOD.replace("public boolean isActive() { return true; }", "");
        assertThat(check("Product.java", atLimit)).isEmpty();
    }

    @Test
    void flagsRecordWithTooManyComponents() {
        String source = "package demo.model;\npublic record Big(String a, String b, String c, String d, String e) {}\n";

        assertThat(check("Big.java", source)).hasSize(1);
    }

    @Test
    void acceptsSmallRecord() {
        String source = "package demo.model;\npublic record Small(String a, String b) {}\n";

        assertThat(check("Small.java", source)).isEmpty();
    }

    @Test
    void ignoresTypesOutsideTheModelPackage() {
        assertThat(check("Product.java", GOD.replace("demo.model", "demo.service"))).isEmpty();
    }
}
