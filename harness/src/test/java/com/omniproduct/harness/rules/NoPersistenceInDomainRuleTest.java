package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NoPersistenceInDomainRuleTest {

    private static List<Violation> check(String fixturePackage) {
        var classes = new ClassFileImporter().importPackages(fixturePackage);
        return new NoPersistenceInDomainRule("..model..").check(SourceSet.ofSources(Map.of(), classes));
    }

    @Test
    void flagsJpaAnnotationsOnDomainTypes() {
        List<Violation> violations = check("com.omniproduct.harness.fixtures.bad");

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.filePath().equals("PersistentThing.java") && v.line() > 0);
        assertThat(violations).allMatch(v -> v.message().contains("PersistentThing"));
    }

    @Test
    void acceptsDomainWithoutPersistence() {
        assertThat(check("com.omniproduct.harness.fixtures.good")).isEmpty();
    }
}
