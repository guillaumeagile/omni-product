package com.omniproduct.harness;

import java.nio.charset.StandardCharsets;
import java.util.List;

// Mesures du code que l'étudiant fait évoluer (model et service) : tokens à lire, complexité cyclomatique totale.
// Ce sont les mesures de l'arbitre : moins de tokens et moins de branches, c'est un code plus facile à lire pour un agent.
// Déterministe : même code en entrée, mêmes chiffres en sortie.
// ck : mesures CK, affichées sans points (absentes d'un rapport produit avant l'essai CK).
public record Metrics(long tokens, int complexity, int maxComplexity, CkMetrics ck) {

    public Metrics(long tokens, int complexity, int maxComplexity) {
        this(tokens, complexity, maxComplexity, null);
    }

    private static final List<String> PERIMETER = List.of("com.omniproduct.model", "com.omniproduct.service");

    public static Metrics of(SourceSet sources) {
        List<SourceFile> files = sources.files().stream().filter(Metrics::inPerimeter).toList();
        List<Integer> complexities = Complexity.of(files);
        return new Metrics(
                files.stream().mapToLong(file -> Tokens.count(new String(file.bytes(), StandardCharsets.UTF_8))).sum(),
                complexities.stream().mapToInt(Integer::intValue).sum(),
                complexities.stream().mapToInt(Integer::intValue).max().orElse(0),
                CkMetrics.of(files));
    }

    private static boolean inPerimeter(SourceFile file) {
        return PERIMETER.stream().anyMatch(pkg -> file.packageName().startsWith(pkg));
    }
}
