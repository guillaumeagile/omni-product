package com.omniproduct.harness.referee;

import com.omniproduct.harness.CkMetrics;
import com.omniproduct.harness.HarnessReport;
import com.omniproduct.harness.Metrics;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

// Les mesures CK dans le score et dans le commentaire de l'arbitre. Chaque mesure rapporte des points quand elle
// s'améliore (plafond par mesure, pas de plafond total : c'est du bonus). Une mesure qui se dégrade ne coûte rien,
// c'est affiché, sauf le couplage total (CBO total) : -1 point par 5 ajoutés, sans plafond.
// Le nombre de classes est affiché sans points : découper ne vaut rien en soi, c'est le couplage et la cohésion qui
// disent si le découpage est bon. Vide si l'un des deux rapports n'a pas de mesures CK.
final class CkTable {

    // step : amélioration qui vaut 1 point ; cap : points maximum ; lowerIsBetter : sens de l'amélioration.
    // step = 0 : affiché sans points. penaltyStep : dégradation qui coûte 1 point, sans plafond ; 0 : la dégradation ne
    // coûte rien.
    record Row(String label, String meaning, ToDoubleFunction<CkMetrics> measure, double step, int cap, boolean lowerIsBetter,
               double penaltyStep) {

        Row(String label, String meaning, ToDoubleFunction<CkMetrics> measure, double step, int cap, boolean lowerIsBetter) {
            this(label, meaning, measure, step, cap, lowerIsBetter, 0);
        }

        int points(CkMetrics before, CkMetrics after) {
            double gain = lowerIsBetter
                    ? measure.applyAsDouble(before) - measure.applyAsDouble(after)
                    : measure.applyAsDouble(after) - measure.applyAsDouble(before);
            // Arrondi avant la division : 0.90 - 0.75 doit valoir 0.15, pas 0.1499999.
            double rounded = Math.round(gain * 1000) / 1000.0;
            int reward = (int) Math.min(cap, Math.max(0, Math.floor(rounded / step)));
            int penalty = penaltyStep == 0 ? 0 : (int) Math.max(0, Math.floor(-rounded / penaltyStep));
            return step == 0 ? 0 : reward - penalty;
        }
    }

    static final List<Row> ROWS = List.of(
            new Row("Classes", "not scored: splitting is not a goal in itself", CkMetrics::classes, 0, 0, true),
            new Row("CBO total (coupling)", "+1 per 2 removed, up to +5; -1 per 5 added, no limit", CkMetrics::coupling, 2, 5, true, 5),
            new Row("CBO max", "+1 per 2 removed, up to +4", CkMetrics::maxCoupling, 2, 4, true),
            new Row("WMC max (class complexity)", "+1 per 2 removed, up to +5", CkMetrics::maxWmc, 2, 5, true),
            new Row("LCOM* max (lack of cohesion, 0..1)", "+1 per 0.1 removed, up to +4", CkMetrics::maxLcom, 0.1, 4, true),
            new Row("Mutable fields (non final)", "+1 per 2 removed, up to +5", CkMetrics::mutableFields, 2, 5, true),
            new Row("Assignments", "+1 per 2 removed, up to +4", CkMetrics::assignments, 2, 4, true),
            new Row("Loops", "+1 per loop removed, up to +4", CkMetrics::loops, 1, 4, true),
            new Row("Lambdas", "+2 per lambda added, up to +10", CkMetrics::lambdas, 0.5, 10, false),
            new Row("Max nesting", "+1 per level removed, up to +4", CkMetrics::maxNesting, 1, 4, true));

    private CkTable() {
    }

    static int points(Optional<HarnessReport> base, HarnessReport head) {
        return pair(base, head)
                .map(pair -> ROWS.stream().mapToInt(row -> row.points(pair.before(), pair.after())).sum())
                .orElse(0);
    }

    // counted : faux pour une PR refusée, dont les points CK ne comptent pas.
    static List<String> lines(Optional<HarnessReport> base, HarnessReport head, boolean counted) {
        return pair(base, head).map(pair -> table(pair.before(), pair.after(), points(base, head), counted)).orElse(List.of());
    }

    private record Pair(CkMetrics before, CkMetrics after) {
    }

    private static Optional<Pair> pair(Optional<HarnessReport> base, HarnessReport head) {
        return ck(base).flatMap(before -> ck(Optional.of(head)).map(after -> new Pair(before, after)));
    }

    private static Optional<CkMetrics> ck(Optional<HarnessReport> report) {
        return report.map(HarnessReport::metrics).map(Metrics::ck);
    }

    private static List<String> table(CkMetrics before, CkMetrics after, int total, boolean counted) {
        return Stream.of(
                        Stream.of("<details><summary>📐 CK metrics of model/ and service/: " + signed(total)
                                        + (counted ? "" : ", not counted: the PR is not accepted") + "</summary>", "",
                                "| Metric | Base | This PR | Δ | Points | Rule |", "|---|---|---|---|---|---|"),
                        ROWS.stream().map(row -> row(row, before, after)),
                        Stream.of("", "Measured by [CK](https://github.com/mauricioaniche/ck) 0.7.0. "
                                + "A metric that gets worse costs no points, except CBO total (-1 per 5 added); read why in Files changed.", "</details>"))
                .flatMap(lines -> lines)
                .toList();
    }

    private static String row(Row row, CkMetrics before, CkMetrics after) {
        double from = row.measure().applyAsDouble(before);
        double to = row.measure().applyAsDouble(after);
        int points = row.points(before, after);
        return "| " + row.label() + " | " + format(from) + " | " + format(to) + " | " + delta(to - from) + " | "
                + (points == 0 ? "" : signed(points)) + " | " + row.meaning() + " |";
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.2f", value);
    }

    private static String delta(double value) {
        return Math.abs(value) < 1e-9 ? "=" : (value > 0 ? "+" : "") + format(Math.round(value * 100) / 100.0);
    }

    private static String signed(int points) {
        return points > 0 ? "+" + points : String.valueOf(points);
    }
}
