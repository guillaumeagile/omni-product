package com.omniproduct.harness;

import com.omniproduct.harness.rules.NoGodClassRule;
import com.omniproduct.harness.rules.NoPersistenceInDomainRule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HarnessMain {

    private static final String USAGE =
            "Usage: harness [--step 1.1] --classes <dossier-de-classes> <dossier-de-sources> [autres-sources...]";

    private HarnessMain() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    // 0 = tout passe, 1 = au moins une règle ERROR échoue, 2 = mauvais usage ou entrée inutilisable.
    public static int run(String[] args) {
        String step = "all";
        List<Path> sourceRoots = new ArrayList<>();
        List<Path> classRoots = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--step" -> {
                    if (++i >= args.length) {
                        return usage("--step requires a value: 1.1");
                    }
                    step = args[i];
                }
                case "--classes" -> {
                    if (++i >= args.length) {
                        return usage("--classes requires a directory");
                    }
                    classRoots.add(Path.of(args[i]));
                }
                default -> sourceRoots.add(Path.of(args[i]));
            }
        }
        if (sourceRoots.isEmpty() || classRoots.isEmpty()) {
            return usage(USAGE);
        }
        if (!step.equals("1.1") && !step.equals("all")) {
            return usage("Unknown --step value '" + step + "'. Use 1.1 or omit.");
        }

        // Un dossier de classes absent ou vide signifie "le code ne compile pas" : jamais un faux PASS.
        for (Path classRoot : classRoots) {
            if (!hasClassFiles(classRoot)) {
                System.err.println("No .class files under " + classRoot + " - compile the project first (./gradlew classes).");
                return 2;
            }
        }
        SourceSet sources = SourceSet.load(sourceRoots, classRoots);
        if (sources.files().isEmpty()) {
            System.err.println("No .java files found under the given source path(s).");
            return 2;
        }

        List<HarnessRule> rules = List.of(
                new NoGodClassRule(4, pkg -> pkg.contains(".model")),
                new NoPersistenceInDomainRule("..model.."));

        System.out.println("CUPID step " + step + " harness - deterministic structural checks");
        System.out.println("Analyzed " + sources.files().size() + " source file(s), "
                + sources.classes().size() + " class(es)");
        System.out.println();

        boolean pass = true;
        for (HarnessRule rule : rules) {
            List<Violation> violations = rule.check(sources).stream()
                    .sorted(Comparator.comparing(Violation::filePath).thenComparingInt(Violation::line))
                    .toList();
            boolean warning = rule.severity() == HarnessSeverity.WARNING;
            String status = violations.isEmpty() ? "PASS" : warning ? "WARN" : "FAIL";
            if (!violations.isEmpty() && !warning) {
                pass = false;
            }
            System.out.println("[" + status + "] " + rule.id() + " - " + rule.name());
            for (Violation v : violations) {
                System.out.println("    " + v.filePath() + ":" + v.line() + "  " + v.message());
            }
        }
        System.out.println();
        System.out.println(pass ? "HARNESS RESULT: PASS" : "HARNESS RESULT: FAIL");
        return pass ? 0 : 1;
    }

    private static boolean hasClassFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return false;
        }
        try (var stream = Files.walk(root)) {
            return stream.anyMatch(p -> p.toString().endsWith(".class"));
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private static int usage(String message) {
        System.err.println(message);
        return 2;
    }
}
