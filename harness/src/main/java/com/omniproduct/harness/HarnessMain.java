package com.omniproduct.harness;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class HarnessMain {

    private static final String USAGE =
            "Usage: harness [--step 1] --classes <dossier-de-classes> <dossier-de-sources> [autres-sources...]";

    private HarnessMain() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    private enum Status { PASS, WARN, FAIL }

    private record Options(String step, List<Path> sources, List<Path> classes) {

        static Options parse(List<String> args) {
            return parse(args, new Options("all", List.of(), List.of()));
        }

        private static Options parse(List<String> args, Options acc) {
            if (args.isEmpty()) {
                return acc;
            }
            String head = args.get(0);
            if (head.equals("--step") || head.equals("--classes")) {
                if (args.size() < 2) {
                    throw new IllegalArgumentException(head + " requires a value");
                }
                Options next = head.equals("--step") ? acc.withStep(args.get(1)) : acc.withClasses(Path.of(args.get(1)));
                return parse(args.subList(2, args.size()), next);
            }
            return parse(args.subList(1, args.size()), acc.withSource(Path.of(head)));
        }

        Options withStep(String value) {
            return new Options(value, sources, classes);
        }

        Options withSource(Path path) {
            return new Options(step, Stream.concat(sources.stream(), Stream.of(path)).toList(), classes);
        }

        Options withClasses(Path path) {
            return new Options(step, sources, Stream.concat(classes.stream(), Stream.of(path)).toList());
        }

        void validate() {
            if (sources.isEmpty() || classes.isEmpty()) {
                throw new IllegalArgumentException(USAGE);
            }
            if (!Steps.names().contains(step)) {
                throw new IllegalArgumentException("Unknown --step value '" + step + "'. Use 1 or omit.");
            }
        }
    }

    private record Outcome(HarnessRule rule, List<Violation> violations) {

        Status status() {
            if (violations.isEmpty()) {
                return Status.PASS;
            }
            return rule.severity() == HarnessSeverity.WARNING ? Status.WARN : Status.FAIL;
        }

        void print() {
            System.out.println("[" + status() + "] " + rule.id() + " - " + rule.name());
            violations.forEach(v -> System.out.println("    " + v.filePath() + ":" + v.line() + "  " + v.message()));
        }
    }

    // 0 = tout passe, 1 = au moins une règle ERROR échoue, 2 = mauvais usage ou entrée inutilisable.
    public static int run(String[] args) {
        Options options;
        try {
            options = Options.parse(List.of(args));
            options.validate();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        // Un dossier de classes absent ou vide signifie "le code ne compile pas" : jamais un faux PASS.
        var uncompiled = options.classes().stream().filter(root -> !hasClassFiles(root)).findFirst();
        if (uncompiled.isPresent()) {
            System.err.println("No .class files under " + uncompiled.get() + " - compile the project first (./gradlew classes).");
            return 2;
        }

        SourceSet sources = SourceSet.load(options.sources(), options.classes());
        if (sources.files().isEmpty()) {
            System.err.println("No .java files found under the given source path(s).");
            return 2;
        }

        List<HarnessRule> rules = Steps.rules(options.step());

        System.out.println("CUPID step " + options.step() + " harness - deterministic structural checks");
        System.out.println("Analyzed " + sources.files().size() + " source file(s), " + sources.classes().size() + " class(es)\n");

        List<Outcome> outcomes = rules.stream()
                .map(rule -> new Outcome(rule, rule.check(sources).stream()
                        .sorted(Comparator.comparing(Violation::filePath).thenComparingInt(Violation::line))
                        .toList()))
                .toList();
        outcomes.forEach(Outcome::print);

        boolean pass = outcomes.stream().noneMatch(outcome -> outcome.status() == Status.FAIL);
        System.out.println("\nHARNESS RESULT: " + (pass ? "PASS" : "FAIL"));
        return pass ? 0 : 1;
    }

    private static boolean hasClassFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.anyMatch(path -> path.toString().endsWith(".class"));
        } catch (IOException e) {
            return false;
        }
    }
}
