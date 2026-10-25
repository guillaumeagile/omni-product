package com.omniproduct.harness;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKNotifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Mesures CK (Maurício Aniche, github.com/mauricioaniche/ck), outil éprouvé, sur model/ et service/ : couplage (CBO),
// complexité par classe (WMC), cohésion (LCOM*), et des signaux de style fonctionnel (champs mutables, affectations,
// boucles, lambdas, imbrication). L'arbitre les note (referee/CkTable), sauf le nombre de classes.
// Limite connue : CK 0.7.0 lit mal Java 16+ (records absents, switch expressions mal comptées) ; fiable en phase 1.
public record CkMetrics(int classes, int coupling, int maxCoupling, int maxWmc, double maxLcom,
                        int mutableFields, int assignments, int loops, int lambdas, int maxNesting) {

    // CK lit un dossier : on y recopie les fichiers du périmètre, pour mesurer exactement ce que mesure le harnais.
    public static CkMetrics of(List<SourceFile> files) {
        List<CKClassResult> classes = measure(files);
        return new CkMetrics(
                classes.size(),
                classes.stream().mapToInt(CKClassResult::getCbo).sum(),
                classes.stream().mapToInt(CKClassResult::getCbo).max().orElse(0),
                classes.stream().mapToInt(CKClassResult::getWmc).max().orElse(0),
                classes.stream().mapToDouble(CKClassResult::getLcomNormalized).filter(Double::isFinite)
                        .map(lcom -> Math.round(lcom * 100) / 100.0).max().orElse(0),
                classes.stream().mapToInt(result -> result.getNumberOfFields() - result.getNumberOfFinalFields()).sum(),
                classes.stream().mapToInt(CKClassResult::getAssignmentsQty).sum(),
                classes.stream().mapToInt(CKClassResult::getLoopQty).sum(),
                classes.stream().mapToInt(CKClassResult::getLambdasQty).sum(),
                classes.stream().mapToInt(CKClassResult::getMaxNestedBlocks).max().orElse(0));
    }

    private static List<CKClassResult> measure(List<SourceFile> files) {
        if (files.isEmpty()) {
            return List.of();
        }
        Path dir = copy(files);
        try {
            // CK rend ses résultats par rappel : seul endroit où l'on accumule, dans une liste sûre entre threads.
            List<CKClassResult> results = new CopyOnWriteArrayList<>();
            new CK(false, 0, false).calculate(dir, new CKNotifier() {
                @Override
                public void notify(CKClassResult result) {
                    results.add(result);
                }

                @Override
                public void notifyError(String file, Exception e) {
                    System.err.println("CK could not measure " + file + ": " + e.getMessage());
                }
            });
            return results.stream().sorted(Comparator.comparing(CKClassResult::getClassName)).toList();
        } finally {
            delete(dir);
        }
    }

    private static Path copy(List<SourceFile> files) {
        try {
            Path dir = Files.createTempDirectory("harness-ck");
            // Chemin reconstruit depuis le package : le chemin reçu peut être absolu ou en "../", il sortirait du dossier.
            files.forEach(file -> write(dir.resolve(file.packageName().replace('.', '/')).resolve(file.fileName()), file.bytes()));
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path target, byte[] bytes) {
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void delete(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Collections.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
