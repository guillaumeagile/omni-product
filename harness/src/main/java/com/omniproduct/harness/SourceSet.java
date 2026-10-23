package com.omniproduct.harness;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.treesitter.TSInputEncoding;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterJava;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// Ce que voient les règles : les sources parsées (moteur AST) et les classes compilées (moteur BYTECODE).
public final class SourceSet {

    private final List<SourceFile> files;
    private final JavaClasses classes;

    public SourceSet(List<SourceFile> files, JavaClasses classes) {
        this.files = files;
        this.classes = classes;
    }

    public List<SourceFile> files() {
        return files;
    }

    public JavaClasses classes() {
        return classes;
    }

    // Ordre trié : le verdict ne dépend pas du système de fichiers.
    public static SourceSet load(List<Path> sourceRoots, List<Path> classRoots) {
        List<Path> javaFiles = sourceRoots.stream()
                .flatMap(SourceSet::walk)
                .filter(p -> p.toString().endsWith(".java"))
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        try (TSParser parser = newParser()) {
            List<SourceFile> parsed = javaFiles.stream()
                    .map(p -> parse(parser, p.toString(), read(p)))
                    .toList();
            return new SourceSet(parsed, new ClassFileImporter().importPaths(classRoots));
        }
    }

    // Pour les tests : sources en mémoire (chemin -> contenu), classes fournies par l'appelant.
    public static SourceSet ofSources(Map<String, String> sourcesByPath, JavaClasses classes) {
        try (TSParser parser = newParser()) {
            List<SourceFile> parsed = sourcesByPath.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> parse(parser, e.getKey(), e.getValue().getBytes(StandardCharsets.UTF_8)))
                    .toList();
            return new SourceSet(parsed, classes);
        }
    }

    private static TSParser newParser() {
        TSParser parser = new TSParser();
        parser.setLanguage(new TreeSitterJava());
        return parser;
    }

    private static SourceFile parse(TSParser parser, String path, byte[] bytes) {
        TSTree tree = parser.parseStringEncoding(null, new String(bytes, StandardCharsets.UTF_8), TSInputEncoding.TSInputEncodingUTF8);
        return new SourceFile(path, bytes, tree);
    }

    private static byte[] read(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stream<Path> walk(Path root) {
        try {
            return Files.walk(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
