package com.omniproduct.harness.rules;

import com.omniproduct.harness.Fixtures;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Les règles sont des heuristiques : changer un nom, une casse, un verbe, un pluriel ou un suffixe ne doit pas suffire à
// leur échapper. Chaque test écrit le contournement le plus évident et exige que la règle le voie quand même.
class HeuristicEvasionTest {

    private static SourceSet source(String code) {
        return SourceSet.ofSources(Map.of("Thing.java", code), null);
    }

    private static boolean isModel(String pkg) {
        return pkg.contains(".model");
    }

    @Test
    void ha5CountsFetchersFluentAccessorsAndSettersNotOnlyGetX() {
        List<Violation> violations = new NoGodClassRule(4, HeuristicEvasionTest::isModel).check(source("""
                package demo.model;

                public class Sneaky {
                    public String fetchName() { return null; }
                    public String slug() { return null; }
                    public int loadStock() { return 0; }
                    public double price() { return 0; }
                    public void setActive(boolean active) { }
                }
                """));

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).message()).contains("5 public properties");
    }

    @Test
    void ha5CountsAReadAndWriteOfTheSamePropertyOnce() {
        assertThat(new NoGodClassRule(4, HeuristicEvasionTest::isModel).check(source("""
                package demo.model;

                public class Tidy {
                    public String getName() { return null; }
                    public void setName(String name) { }
                    public String name() { return null; }
                    public String toString() { return ""; }
                    public String getProductID() { return null; }
                }
                """))).isEmpty();
    }

    @Test
    void ha8SeesStoredFormFieldsAndSyncMethodsWithoutAnyPersistenceImport() {
        List<Violation> violations = new NoPersistenceInDomainRule("..model..", HeuristicEvasionTest::isModel)
                .check(Fixtures.load("ha8.shadow"));

        assertThat(violations).extracting(Violation::message).anyMatch(m -> m.contains("itemsJson"))
                .anyMatch(m -> m.contains("tags_csv")).anyMatch(m -> m.contains("syncItems")).anyMatch(m -> m.contains("hydrateTags"));
        assertThat(violations).hasSize(4);
    }

    @Test
    void ha10SeesCollaboratorsThatAreNotCalledService() {
        List<Violation> violations = new NoServicesInEntityConstructorRule("..model..").check(Fixtures.load("ha10.evasion"));

        assertThat(violations).extracting(Violation::message)
                .anyMatch(m -> m.contains("PriceManager"))   // suffixe de collaborateur
                .anyMatch(m -> m.contains("StockClient"))    // caché dans un générique : Supplier<StockClient>
                .anyMatch(m -> m.contains("Wiring"));        // reconnu à son annotation @Component
        assertThat(violations).hasSize(3);
    }

    @Test
    void ha10TreatsAnEntityWhoseOnlyIdentityIsItsOwnNamedIdAsAnEntity() {
        // Basket n'a aucun champ `id` ni Identifiable : son identité est `basketId`. Elle doit rester une entité.
        assertThat(new NoServicesInEntityConstructorRule("..model..").check(Fixtures.load("ha10.evasion"))).isNotEmpty();
    }

    @Test
    void ha11RecognisesIdentifiersWhateverTheCasingOrSeparator() {
        List<Violation> violations = new RichIdentifierRule(HeuristicEvasionTest::isModel).check(source("""
                package demo.model;

                public class Sneaky {
                    private String productID;
                    private String product_id;
                    private long idProduct;
                    private String uuid;
                    private String identifier;
                    private String valid;
                }
                """));

        assertThat(violations).extracting(v -> v.line()).containsExactly(4, 5, 6, 7, 8);
    }

    @Test
    void ha11FlagsAnyIdentifierTypeThatIsNotUuidOrUlidNotOnlyKnownPoorOnes() {
        List<Violation> violations = new RichIdentifierRule(HeuristicEvasionTest::isModel).check(source("""
                package demo.model;

                public class Sneaky {
                    private CharSequence id;
                    private java.math.BigDecimal orderId;
                    private double ownerId;
                    private java.util.UUID goodId;
                }
                """));

        assertThat(violations).extracting(v -> v.line()).containsExactly(4, 5, 6);
    }

    @Test
    void ha11FlagsNonV7UuidsWhereverTheyAreBuilt() {
        List<Violation> violations = new RichIdentifierRule(HeuristicEvasionTest::isModel).check(source("""
                package demo.model;

                import java.util.UUID;
                import static java.util.UUID.randomUUID;

                public class Sneaky {
                    static UUID newId() { return UUID.randomUUID(); }
                    static UUID other() { return randomUUID(); }
                    static UUID named() { return UUID.nameUUIDFromBytes(new byte[0]); }
                    static UUID v1() { return UuidCreator.getTimeBased(); }
                    static java.util.function.Supplier<UUID> ref() { return UUID::randomUUID; }
                    static UUID good() { return UuidCreator.getTimeOrderedEpoch(); }
                }
                """));

        assertThat(violations).extracting(v -> v.line()).containsExactly(7, 8, 9, 10, 11);
    }
}
