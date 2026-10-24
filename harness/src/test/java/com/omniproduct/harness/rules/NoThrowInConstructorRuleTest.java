package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NoThrowInConstructorRuleTest {

    private static List<Violation> check(String source) {
        return new NoThrowInConstructorRule(pkg -> pkg.contains(".model"))
                .check(SourceSet.ofSources(Map.of("Thing.java", source), null));
    }

    @Test
    void flagsThrowInAConstructorAndACompactConstructorWithTheirLines() {
        String source = """
                package demo.model;

                public class Thing {
                    public Thing(String name) {
                        if (name == null) throw new IllegalArgumentException();
                    }
                }

                record Pair(int a) {
                    Pair {
                        if (a < 0) throw new IllegalArgumentException();
                    }
                }
                """;

        assertThat(check(source)).extracting(Violation::line).containsExactly(5, 11);
    }

    @Test
    void flagsGuardCallsSuchAsRequireNonNullAndCheckArgument() {
        String source = """
                package demo.model;

                import static java.util.Objects.requireNonNull;

                public class Thing {
                    public Thing(String name) {
                        requireNonNull(name);
                        Preconditions.checkArgument(!name.isBlank());
                        this.name = name;
                    }
                    private final String name;
                }
                """;

        assertThat(check(source)).hasSize(2);
    }

    @Test
    void acceptsThrowOutsideConstructorsAndUnrelatedNames() {
        String source = """
                package demo.model;

                public class Thing {
                    public Thing(String name) {
                        this.name = checkout(name);
                    }
                    private final String name;
                    String checkout(String name) {
                        if (name == null) throw new IllegalStateException();
                        return name;
                    }
                }
                """;

        assertThat(check(source)).isEmpty();
    }
}
