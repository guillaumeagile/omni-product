package com.omniproduct.harness.rules;

import com.omniproduct.harness.Fixtures;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoPersistenceInDomainRuleTest {

    private static List<Violation> check(String fixture) {
        return new NoPersistenceInDomainRule("..model..", pkg -> pkg.contains(".model")).check(Fixtures.load(fixture));
    }

    @Test
    void flagsJpaAnnotationsOnDomainTypes() {
        List<Violation> violations = check("bad");

        assertThat(violations).isNotEmpty();
        // @Entity est ligne 7 et @Id ligne 9 de PersistentThing.java : la ligne vient de l'AST, pas du bytecode.
        assertThat(violations).anyMatch(v -> v.message().contains("jakarta.persistence.Entity") && v.line() == 7);
        assertThat(violations).anyMatch(v -> v.message().contains("jakarta.persistence.Id") && v.line() == 9);
        assertThat(violations).allMatch(v -> v.message().contains("PersistentThing"));
    }

    @Test
    void acceptsDomainWithoutPersistence() {
        assertThat(check("good")).isEmpty();
    }

    // Témoin HA8 : un modèle qui ne sait pas comment il est stocké, avec des noms proches du vocabulaire de stockage.
    @Test
    void acceptsAModelThatOnlyLooksLikeStorageButIsNot() {
        assertThat(check("ha8.good")).isEmpty();
    }
}
