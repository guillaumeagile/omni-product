package com.omniproduct.harness;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Complexité cyclomatique de chaque méthode et constructeur, calculée par PMD (règle CyclomaticComplexity, outil
// éprouvé) plutôt que par un calcul maison. Le seuil de rapport est 1 (pmd-cyclomatic.xml) : chaque méthode est
// rapportée, avec sa complexité dans le message ("... has a cyclomatic complexity of 3.").
public final class Complexity {

    private static final Pattern VALUE = Pattern.compile("cyclomatic complexity of (\\d+)");

    private Complexity() {
    }

    public static List<Integer> of(List<SourceFile> files) {
        if (files.isEmpty()) {
            return List.of();
        }
        PMDConfiguration config = new PMDConfiguration();
        config.setDefaultLanguageVersion(LanguageRegistry.PMD.getLanguageById("java").getVersion("21"));
        config.addRuleSet("pmd-cyclomatic.xml");
        config.setIgnoreIncrementalAnalysis(true);
        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            files.forEach(file -> pmd.files().addSourceFile(FileId.fromPathLikeString(file.path()),
                    new String(file.bytes(), StandardCharsets.UTF_8)));
            Report report = pmd.performAnalysisAndCollectReport();
            report.getProcessingErrors().forEach(error ->
                    System.err.println("PMD could not measure " + error.getFileId().getOriginalPath() + ": " + error.getMsg()));
            return report.getViolations().stream()
                    .map(RuleViolation::getDescription)
                    .map(VALUE::matcher)
                    .filter(Matcher::find)
                    .map(match -> Integer.parseInt(match.group(1)))
                    .toList();
        }
    }
}
