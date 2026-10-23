package com.omniproduct.harness;

import com.tngtech.archunit.core.importer.ClassFileImporter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Un jeu de fixtures = les sources d'un dossier (pour l'AST) + les classes compilées du même package (pour ArchUnit).
public final class Fixtures {

    private static final Path ROOT = Path.of("src/test/java/com/omniproduct/harness/fixtures");

    private Fixtures() {
    }

    // fixtureName : "ha4.bad" -> fixtures/ha4/bad et package com.omniproduct.harness.fixtures.ha4.bad
    public static SourceSet load(String fixtureName) {
        Path dir = ROOT.resolve(fixtureName.replace('.', '/'));
        try (Stream<Path> files = Files.walk(dir)) {
            Map<String, String> sources = files
                    .filter(file -> file.toString().endsWith(".java"))
                    .collect(Collectors.toMap(Path::toString, Fixtures::read));
            return SourceSet.ofSources(sources,
                    new ClassFileImporter().importPackages("com.omniproduct.harness.fixtures." + fixtureName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
