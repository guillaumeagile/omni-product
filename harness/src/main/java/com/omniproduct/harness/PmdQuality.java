package com.omniproduct.harness;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Mesures de qualité de model/ et service/ par PMD (essai, branche testPMD), à la place de CK qui ne voit pas les
// records : couplage (FAN_OUT), complexité par classe (WMC), cohésion (TCC, la plus faible), complexité cognitive,
// et des signaux de style fonctionnel (champs mutables, affectations, boucles, lambdas). Une ligne par classe,
// record ou enum (QualityMetricsRule), agrégée ici. minTcc vaut 1 quand aucune classe n'a de cohésion mesurable.
public record PmdQuality(int types, int fanOut, int maxFanOut, int maxWmc, double minTcc, int mutableFields,
                         int assignments, int loops, int lambdas, int cognitive, int maxCognitive) {

    // Mesures d'une classe, lues dans le message de QualityMetricsRule.
    record TypeQuality(String type, int wmc, double tcc, int fanOut, int mutable, int assignments, int loops,
                       int lambdas, int cognitive, int cognitiveMax) {

        static TypeQuality parse(String message) {
            Map<String, String> values = Arrays.stream(message.split(" "))
                    .filter(pair -> pair.contains("="))
                    .map(pair -> pair.split("=", 2))
                    .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
            return new TypeQuality(values.get("type"), integer(values, "wmc"),
                    values.get("tcc").equals("na") ? Double.NaN : Double.parseDouble(values.get("tcc")),
                    integer(values, "fanout"), integer(values, "mutable"), integer(values, "assignments"),
                    integer(values, "loops"), integer(values, "lambdas"), integer(values, "cognitive"), integer(values, "cognitiveMax"));
        }

        private static int integer(Map<String, String> values, String key) {
            return Integer.parseInt(values.get(key));
        }
    }

    public static PmdQuality of(List<SourceFile> files) {
        return aggregate(measure(files));
    }

    static PmdQuality aggregate(List<TypeQuality> types) {
        return new PmdQuality(
                types.size(),
                types.stream().mapToInt(TypeQuality::fanOut).sum(),
                types.stream().mapToInt(TypeQuality::fanOut).max().orElse(0),
                types.stream().mapToInt(TypeQuality::wmc).max().orElse(0),
                types.stream().mapToDouble(TypeQuality::tcc).filter(Double::isFinite)
                        .map(tcc -> Math.round(tcc * 100) / 100.0).min().orElse(1),
                types.stream().mapToInt(TypeQuality::mutable).sum(),
                types.stream().mapToInt(TypeQuality::assignments).sum(),
                types.stream().mapToInt(TypeQuality::loops).sum(),
                types.stream().mapToInt(TypeQuality::lambdas).sum(),
                types.stream().mapToInt(TypeQuality::cognitive).sum(),
                types.stream().mapToInt(TypeQuality::cognitiveMax).max().orElse(0));
    }

    static List<TypeQuality> measure(List<SourceFile> files) {
        if (files.isEmpty()) {
            return List.of();
        }
        PMDConfiguration config = new PMDConfiguration();
        config.setDefaultLanguageVersion(LanguageRegistry.PMD.getLanguageById("java").getVersion("21"));
        config.addRuleSet("pmd-quality.xml");
        config.setIgnoreIncrementalAnalysis(true);
        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            files.forEach(file -> pmd.files().addSourceFile(FileId.fromPathLikeString(file.path()),
                    new String(file.bytes(), StandardCharsets.UTF_8)));
            Report report = pmd.performAnalysisAndCollectReport();
            report.getProcessingErrors().forEach(error ->
                    System.err.println("PMD could not measure " + error.getFileId().getOriginalPath() + ": " + error.getMsg()));
            return report.getViolations().stream()
                    .map(RuleViolation::getDescription)
                    .filter(message -> message.startsWith("quality "))
                    .map(TypeQuality::parse)
                    .toList();
        }
    }
}
