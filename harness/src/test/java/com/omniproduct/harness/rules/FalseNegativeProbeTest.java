package com.omniproduct.harness.rules;

import com.omniproduct.harness.Fixtures;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.UbiquitousLanguageMap;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Passe « faux négatifs » : pour chaque règle AST, le contournement le plus plausible d'un étudiant. Chaque test dit ce
// qui DOIT être signalé (ou, pour un contre-exemple sain, ce qui ne doit PAS l'être). Un test rouge = un trou réel.
class FalseNegativeProbeTest {

    private static SourceSet source(String code) {
        return SourceSet.ofSources(Map.of("Probe.java", code), null);
    }

    private static boolean isModel(String pkg) {
        return pkg.contains(".model");
    }

    private static List<Violation> ha5(String code) {
        return new NoGodClassRule(4, FalseNegativeProbeTest::isModel).check(source(code));
    }

    private static List<Violation> ha6(String code) {
        return new SingleConcernRule(UbiquitousLanguageMap.load(), 1, FalseNegativeProbeTest::isModel).check(source(code));
    }

    private static List<Violation> ha11(String code) {
        return new RichIdentifierRule(FalseNegativeProbeTest::isModel).check(source(code));
    }

    // HA5 -----------------------------------------------------------------------------------------------------

    @Test
    void ha5SeesPropertiesDeclaredOnAnInterface() {
        // Dans une interface, une méthode sans modifieur est publique : l'API est tout autant une surface.
        assertThat(ha5("""
                package demo.model;
                public interface View {
                    String getA(); String getB(); String getC(); String getD(); String getE();
                }
                """)).hasSize(1);
    }

    @Test
    void ha5IgnoresPublicStaticConstants() {
        assertThat(ha5("""
                package demo.model;
                public class Constants {
                    public static final int A = 1, B = 2, C = 3, D = 4, E = 5;
                }
                """)).isEmpty();
    }

    // HA6 -----------------------------------------------------------------------------------------------------

    @Test
    void ha6MatchesAMultiWordKeywordWhateverTheWordOrder() {
        // « display label » est un mot-clé de Catalog ; « label for display » en contient les deux mots.
        assertThat(ha6("""
                package demo.model;
                public class Mixed {
                    public String getLabelForDisplay() { return null; }
                    public int getStock() { return 0; }
                }
                """)).hasSize(1);
    }

    @Test
    void ha6KnowsTheFrenchTermForVat() {
        // Le legacy est en français : TVA doit compter comme Pricing, comme VAT.
        assertThat(ha6("""
                package demo.model;
                public class Mixed {
                    public double getTva() { return 0; }
                    public int getStock() { return 0; }
                }
                """)).hasSize(1);
    }

    // HA8 -----------------------------------------------------------------------------------------------------

    @Test
    void ha8SeesFlushReloadAndAStoredFormFieldWithoutAStandardSuffix() {
        // Fixture compilée : la règle a besoin des classes pour sa partie bytecode.
        assertThat(new NoPersistenceInDomainRule("..model..", FalseNegativeProbeTest::isModel)
                .check(Fixtures.load("ha8.evasion"))).hasSize(3);
    }

    // HA11 ----------------------------------------------------------------------------------------------------

    @Test
    void ha11DoesNotConfuseAVarDeclaredIdWithAType() {
        // `var` est une inférence : l'identifiant est bien un UUID v7, il ne doit pas être signalé comme « var ».
        assertThat(ha11("""
                package demo.model;
                public class Ok {
                    void f() { var supplierId = UuidCreator.getTimeOrderedEpoch(); }
                }
                """)).isEmpty();
    }

    @Test
    void ha11ChecksTheParameterOfAnIdSetter() {
        // setSupplierId(String value) : le paramètre s'appelle value, mais c'est bien l'identifiant du fournisseur.
        assertThat(ha11("""
                package demo.model;
                public class Carrier {
                    public void setSupplierId(String value) { }
                }
                """)).hasSize(1);
    }

    @Test
    void ha11ChecksTheKeyOfAMapOfEntitiesKeyedById() {
        assertThat(ha11("""
                package demo.model;
                import java.util.Map;
                public class Catalog {
                    private Map<String, Object> productsById;
                }
                """)).hasSize(2).anyMatch(v -> v.message().contains("is a String"));
    }

    @Test
    void ha11ChecksAnOptionalIdentifier() {
        assertThat(ha11("""
                package demo.model;
                import java.util.Optional;
                public class Product {
                    private Optional<String> productId;
                }
                """)).hasSize(1);
    }

    @Test
    void ha11KnowsBusinessIdentifiersLikeSiren() {
        // Un SIREN, un SKU, un EAN sont des identifiants métier : un String n'a rien à faire là.
        assertThat(ha11("""
                package demo.model;
                public class Supplier {
                    private String siren;
                    private String sku;
                }
                """)).hasSize(2);
    }

    @Test
    void ha11FlagsAUuidOlderThanV7() {
        assertThat(ha11("""
                package demo.model;
                import java.util.UUID;
                public class Product {
                    UUID make() { return UuidCreator.getTimeOrdered(); }
                }
                """)).hasSize(1);
    }
}
