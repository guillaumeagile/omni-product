package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NestedTypeInEntityRuleTest {

    private static List<Violation> check(String... files) {
        Map<String, String> sources = new java.util.LinkedHashMap<>();
        for (int i = 0; i < files.length; i++) {
            sources.put("File" + i + ".java", files[i]);
        }
        return new NestedTypeInEntityRule(pkg -> pkg.contains(".model")).check(SourceSet.ofSources(sources, null));
    }

    private static String entity(String header, String body) {
        return """
                package demo.model;

                public class %s {
                    private final String name;
                %s
                }
                """.formatted(header, body);
    }

    // --- Fixture mauvaise : le départ de la phase 3 (Product.Warehouse, Supplier.Contact...) ---

    @Test
    void flagsEveryNestedTypeOfAnEntityWithItsLine() {
        List<Violation> violations = check("""
                package demo.model;

                public class Supplier implements Identifiable {
                    public record Contact(String email) {}

                    public record Address(String country) {}
                }
                """);

        assertThat(violations).extracting(Violation::line).containsExactly(4, 6);
        assertThat(violations.get(0).message()).contains("'Supplier.Contact'", "Contact.java");
    }

    @Test
    void mentionsTheNameCollisionWithAnotherType() {
        List<Violation> violations = check("""
                package demo.model;

                public class Product implements Identifiable {
                    public record Supplier(String name) {}
                }
                """, """
                package demo.model;

                public class Supplier implements Identifiable {
                }
                """);

        assertThat(violations).singleElement().satisfies(v -> assertThat(v.message()).contains("shares its name"));
    }

    // --- Fixtures bonnes ---

    @Test
    void acceptsAnEntityWhoseConceptsLiveInTheirOwnFiles() {
        assertThat(check("""
                package demo.model;

                public class Product implements Identifiable {
                    private final Warehouse warehouse = null;
                }
                """, """
                package demo.model;

                public record Warehouse(String location) {}
                """)).isEmpty();
    }

    @Test
    void ignoresNestedTypesOfAClassThatIsNotAnEntity() {
        assertThat(check(entity("Helper", "    record Pair(String left, String right) {}"))).isEmpty();
    }

    @Test
    void ignoresARecordThatCarriesNestedRecordsBecauseItIsNotAnEntityClass() {
        assertThat(check("""
                package demo.model;

                public record Price(Amount amount) {
                    public record Amount(int value) {}
                }
                """)).isEmpty();
    }

    @Test
    void staysOutOfOtherPackages() {
        assertThat(check("""
                package demo.service;

                public class Product implements Identifiable {
                    record Pair(String left, String right) {}
                }
                """)).isEmpty();
    }

    // --- Esquives : un autre marqueur, un autre genre de type, une autre visibilité ---

    @ParameterizedTest
    @ValueSource(strings = {
            "record Pair(String left, String right) {}",
            "public static record Pair(String left, String right) {}",
            "private record Pair(String left, String right) {}",
            "static class Pair { }",
            "public enum Pair { LEFT, RIGHT }",
            "interface Pair { }",
            "sealed interface Pair permits A { record A() implements Pair {} }"})
    void flagsEveryKindAndVisibilityOfNestedType(String nested) {
        assertThat(check("""
                package demo.model;

                public class Product implements Identifiable {
                    %s
                }
                """.formatted(nested))).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "implements IDENTIFIABLE", "implements HasIdentifiable", "implements Comparable<X>, Identifiable"})
    void recognisesTheEntityBySeveralSpellingsOfTheMarker(String implementsClause) {
        assertThat(check("""
                package demo.model;

                public class Product %s {
                    record Pair(String left, String right) {}
                }
                """.formatted(implementsClause))).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ProductId id;", "String id;", "String productId;", "String PRODUCT_ID;", "String product_id;"})
    void recognisesTheEntityByItsIdentityFieldEvenWithoutTheMarker(String field) {
        assertThat(check("""
                package demo.model;

                public class Product {
                    %s
                    record Pair(String left, String right) {}
                }
                """.formatted(field))).hasSize(1);
    }

    @Test
    void flagsATypeNestedTwoLevelsDeep() {
        assertThat(check("""
                package demo.model;

                public class Product implements Identifiable {
                    public static class Holder {
                        record Inner(String value) {}
                    }
                }
                """)).hasSize(2);
    }

    @Test
    void isAnErrorThatFailsTheHarness() {
        assertThat(new NestedTypeInEntityRule(pkg -> true).severity().name()).isEqualTo("ERROR");
        assertThat(new NestedTypeInEntityRule(pkg -> true).id()).isEqualTo("HA18");
    }
}
