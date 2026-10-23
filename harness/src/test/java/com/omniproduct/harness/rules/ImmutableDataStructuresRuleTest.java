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
    void flagsClassSetterAndMutableCollectionsWithTheirLines() {
        List<Violation> violations = check(MUTABLE);

        assertThat(violations).anySatisfy(v -> {
            assertThat(v.line()).isEqualTo(6);
            assertThat(v.message()).contains("'Thing' is a class");
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

    @Test
    void ignoresTypesOutsideTheModelPackage() {
        assertThat(check(MUTABLE.replace("demo.model", "demo.service"))).isEmpty();
    }
}
