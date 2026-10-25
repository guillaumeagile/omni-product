package com.omniproduct.harness.referee;

import com.omniproduct.harness.CkMetrics;
import com.omniproduct.harness.HarnessReport;
import com.omniproduct.harness.Metrics;

import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

// Le tableau CK du commentaire de l'arbitre : base, PR, écart, pour chaque mesure. Information seulement, sans points
// (essai, branche testCK). Vide si l'un des deux rapports n'a pas de mesures CK.
final class CkTable {

    private record Row(String label, String meaning, ToDoubleFunction<CkMetrics> measure) {
    }

    private static final List<Row> ROWS = List.of(
            new Row("Classes", "more, smaller classes is the expected effect of a split", CkMetrics::classes),
            new Row("CBO total (coupling)", "lower is better: fewer dependencies between classes", CkMetrics::coupling),
            new Row("CBO max", "lower is better: the most coupled class", CkMetrics::maxCoupling),
            new Row("WMC max (class complexity)", "lower is better: the god class shrinks", CkMetrics::maxWmc),
            new Row("LCOM* max (lack of cohesion, 0..1)", "lower is better: one reason to change", CkMetrics::maxLcom),
            new Row("Mutable fields (non final)", "lower is better: immutable data", CkMetrics::mutableFields),
            new Row("Assignments", "lower is better: less mutation", CkMetrics::assignments),
            new Row("Loops", "lower is better: streams instead of loops", CkMetrics::loops),
            new Row("Lambdas", "functional style indicator", CkMetrics::lambdas),
            new Row("Max nesting", "lower is better: flatter code", CkMetrics::maxNesting));

    private CkTable() {
    }

    static List<String> lines(Optional<HarnessReport> base, HarnessReport head) {
        return ck(base).flatMap(before -> ck(Optional.of(head)).map(after -> table(before, after))).orElse(List.of());
    }

    private static Optional<CkMetrics> ck(Optional<HarnessReport> report) {
        return report.map(HarnessReport::metrics).map(Metrics::ck);
    }

    private static List<String> table(CkMetrics before, CkMetrics after) {
        return Stream.of(
                        Stream.of("<details><summary>📐 CK metrics of model/ and service/ (information, no points)</summary>", "",
                                "| Metric | Base | This PR | Δ | Reading |", "|---|---|---|---|---|"),
                        ROWS.stream().map(row -> row(row, before, after)),
                        Stream.of("", "Measured by [CK](https://github.com/mauricioaniche/ck) 0.7.0.", "</details>"))
                .flatMap(lines -> lines)
                .toList();
    }

    private static String row(Row row, CkMetrics before, CkMetrics after) {
        double from = row.measure().applyAsDouble(before);
        double to = row.measure().applyAsDouble(after);
        return "| " + row.label() + " | " + format(from) + " | " + format(to) + " | " + delta(to - from) + " | " + row.meaning() + " |";
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String delta(double value) {
        return Math.abs(value) < 1e-9 ? "=" : (value > 0 ? "+" : "") + format(Math.round(value * 100) / 100.0);
    }
}
