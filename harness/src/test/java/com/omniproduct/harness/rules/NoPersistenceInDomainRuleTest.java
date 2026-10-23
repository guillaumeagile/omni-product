package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NoPersistenceInDomainRuleTest {

    private static final Path FIXTURE_SOURCES = Path.of("src/test/java/com/omniproduct/harness/fixtures");

    private static List<Violation> check(String fixturePackage) throws IOException {
        var classes = new ClassFileImporter().importPackages(fixturePackage);
        Map<String, String> sources = new HashMap<>();
        try (var files = Files.walk(FIXTURE_SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                sources.put(file.toString(), Files.readString(file));
            }
        }
        return new NoPersistenceInDomainRule("..model..").check(SourceSet.ofSources(sources, classes));
    }

    @Test
    void flagsJpaAnnotationsOnDomainTypes() throws IOException {
        List<Violation> violations = check("com.omniproduct.harness.fixtures.bad");

        assertThat(violations).isNotEmpty();
        // @Entity est ligne 7 et @Id ligne 9 de PersistentThing.java : la ligne vient de l'AST, pas du bytecode.
        assertThat(violations).anyMatch(v -> v.message().contains("jakarta.persistence.Entity") && v.line() == 7);
        assertThat(violations).anyMatch(v -> v.message().contains("jakarta.persistence.Id") && v.line() == 9);
        assertThat(violations).allMatch(v -> v.message().contains("PersistentThing"));
    }

    @Test
    void acceptsDomainWithoutPersistence() throws IOException {
        assertThat(check("com.omniproduct.harness.fixtures.good")).isEmpty();
    }
}
