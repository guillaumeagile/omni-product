package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessSeverity;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IdentifierRulesTest {

    private static SourceSet sources(String... files) {
        java.util.Map<String, String> byPath = new java.util.LinkedHashMap<>();
        for (int i = 0; i < files.length; i++) {
            byPath.put("File" + i + ".java", files[i]);
        }
        return SourceSet.ofSources(byPath, null);
    }

    private static List<Violation> rich(String... files) {
        return new RichIdentifierRule(pkg -> pkg.contains(".model")).check(sources(files));
    }

    private static List<Violation> bare(String... files) {
        return new BareIdentifierIsWeakRule(pkg -> true).check(sources(files));
    }

    private static final String STRING_ID = """
            package demo.model;

            import java.util.List;

            public class Product {
                private String id;
                private List<String> supplierIds;
                public String getId() { return id; }
                public void link(String supplierId) { }
                public String name;
            }
            """;

    @Test
    void flagsStringIdentifiersInFieldsCollectionsGettersAndParameters() {
        List<Violation> violations = rich(STRING_ID);

        assertThat(violations).extracting(Violation::line).containsExactlyInAnyOrder(6, 7, 8, 9);
        assertThat(violations.get(0).message()).contains("is a String");
    }

    @Test
    void acceptsUuidUlidAndRecordsWrappingExactlyOne() {
        assertThat(rich("""
                package demo.model;

                import java.util.UUID;
                import java.util.List;

                public record ProductId(UUID value) { }
                """, """
                package demo.model;

                import java.util.List;

                public class Supplier {
                    private ProductId productId;
                    private List<ProductId> productIds;
                    private java.util.UUID id;
                    public ProductId getProductId() { return productId; }
                }
                """)).isEmpty();
    }

    @Test
    void flagsADeclaredIdentifierTypeThatDoesNotWrapAUuid() {
        List<Violation> violations = rich("package demo.model;\npublic record ProductId(String value) { }\n",
                "package demo.model;\npublic class Supplier {\n    private ProductId productId;\n}\n");

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("not a record wrapping exactly one UUID or Ulid");
    }

    @Test
    void flagsVersion4AndEmptyUuidsBuildingAnIdentifier() {
        List<Violation> violations = rich("""
                package demo.model;

                import java.util.UUID;

                public record ProductId(UUID value) { }
                """, """
                package demo.model;

                import java.util.UUID;

                public class Factory {
                    private UUID id = UUID.randomUUID();
                    public ProductId create() { return new ProductId(UUID.randomUUID()); }
                    public void reset() { this.id = new UUID(0, 0); }
                }
                """);

        assertThat(violations).extracting(Violation::line).containsExactlyInAnyOrder(6, 7, 8);
    }

    @Test
    void flagsFullyQualifiedRandomUuids() {
        assertThat(rich("""
                package demo.model;

                public class Factory {
                    private String id;
                    public void reset() { this.id = java.util.UUID.randomUUID().toString(); }
                }
                """)).anyMatch(v -> v.line() == 5 && v.message().contains("builds a UUID v4"));
    }

    @Test
    void searchMethodsAreNotIdentifiers() {
        assertThat(rich("package demo.model;\npublic class Repo {\n    public Product getById(String x) { return null; }\n}\n")).isEmpty();
    }

    @Test
    void bareIdentifiersWarnEvenWhenTheyAreUuidsButNotWhenDeclaredInSource() {
        List<Violation> violations = bare("""
                package demo.service;

                import java.util.UUID;

                public class Lookup {
                    private UUID supplierId;
                    private ProductId productId;
                }
                """, "package demo.service;\npublic record ProductId(UUID value) { }\n");

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("'supplierId' is a raw UUID");
        assertThat(new BareIdentifierIsWeakRule(pkg -> true).severity()).isEqualTo(HarnessSeverity.WARNING);
    }

    @Test
    void ignoresPackagesOutsideTheFilter() {
        assertThat(rich(STRING_ID.replace("demo.model", "demo.service"))).isEmpty();
    }
}
