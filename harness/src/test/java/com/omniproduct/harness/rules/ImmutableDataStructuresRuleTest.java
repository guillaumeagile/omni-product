package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImmutableDataStructuresRuleTest {

    private static List<Violation> check(String source) {
        return new ImmutableDataStructuresRule(pkg -> pkg.contains(".model"))
                .check(SourceSet.ofSources(Map.of("Thing.java", source), null));
    }

    private static final String MUTABLE = """
            package demo.model;

            import java.util.List;
            import java.util.Map;

            public class Thing {
                private List<String> tags;
                public void setTags(List<String> tags) { this.tags = tags; }
                public Map<String, String> getLabels() { return null; }
            }
            """;

    @Test
    void flagsNonFinalFieldSetterAndMutableCollectionsWithTheirLines() {
        List<Violation> violations = check(MUTABLE);

        assertThat(violations).anySatisfy(v -> {
            assertThat(v.line()).isEqualTo(7);
            assertThat(v.message()).contains("'tags' in 'Thing' is not final");
        });
        assertThat(violations).anySatisfy(v -> {
            assertThat(v.line()).isEqualTo(8);
            assertThat(v.message()).contains("'setTags' is a setter");
        });
        assertThat(violations).filteredOn(v -> v.message().contains("mutable java.util collection"))
                .extracting(Violation::line)
                .containsExactlyInAnyOrder(7, 8, 9);
    }

    @Test
    void acceptsRecordsWithVavrCollections() {
        String immutable = """
                package demo.model;

                import io.vavr.collection.List;
                import io.vavr.collection.Map;

                public record Thing(List<String> tags, Map<String, String> labels) {
                    public Thing withTags(List<String> newTags) { return new Thing(newTags, labels); }
                }
                """;

        assertThat(check(immutable)).isEmpty();
    }

    @Test
    void flagsQualifiedAndWildcardJavaUtilCollections() {
        String source = """
                package demo.model;

                import java.util.*;

                public record Thing(java.util.List<String> a, Set<String> b) {
                }
                """;

        assertThat(check(source)).hasSize(2);
    }

    // Une entité est une classe (HA9) : elle est immuable si tous ses champs sont final, sans setter.
    @Test
    void acceptsAClassWithFinalFieldsOnly() {
        String immutable = """
                package demo.model;

                import io.vavr.collection.List;

                public class Thing implements Identifiable {
                    private static final int LIMIT = 4;
                    private final ThingId id;
                    private final List<String> tags;

                    public Thing(ThingId id, List<String> tags) { this.id = id; this.tags = tags; }
                    public ThingId getId() { return id; }
                    public Thing withTags(List<String> newTags) { return new Thing(id, newTags); }
                }
                """;

        assertThat(check(immutable)).isEmpty();
    }

    // Évasions : plusieurs déclarateurs sur une ligne, un champ statique, volatile, une classe imbriquée.
    @Test
    void flagsEveryNonFinalFieldEvenStaticOrNested() {
        String source = """
                package demo.model;

                public class Thing {
                    private final String kept;
                    private String a, b;
                    static int counter;
                    volatile int version;
                    public Thing(String kept) { this.kept = kept; }

                    public static class Part {
                        private String label;
                    }
                }
                """;

        assertThat(check(source)).extracting(Violation::message)
                .containsExactlyInAnyOrder(
                        "'a' in 'Thing' is not final: an immutable class has final fields only (or make it a record).",
                        "'b' in 'Thing' is not final: an immutable class has final fields only (or make it a record).",
                        "'counter' in 'Thing' is not final: an immutable class has final fields only (or make it a record).",
                        "'version' in 'Thing' is not final: an immutable class has final fields only (or make it a record).",
                        "'label' in 'Part' is not final: an immutable class has final fields only (or make it a record).");
    }

    // Évasion : un champ final ne protège pas le contenu d'un tableau, dans une classe comme dans un record.
    @Test
    void flagsArraysEvenInFinalFieldsAndRecordComponents() {
        String source = """
                package demo.model;

                public class Thing {
                    private final String[] tags;
                    public Thing(String[] tags) { this.tags = tags; }
                }

                record Other(int[][] grid) {}
                """;

        assertThat(check(source)).filteredOn(v -> v.message().contains("is an array"))
                .extracting(Violation::line)
                .containsExactlyInAnyOrder(4, 5, 8);
    }

    @Test
    void ignoresTypesOutsideTheModelPackage() {
        assertThat(check(MUTABLE.replace("demo.model", "demo.service"))).isEmpty();
    }
}
