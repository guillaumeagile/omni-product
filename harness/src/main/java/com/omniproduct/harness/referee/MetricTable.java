package com.omniproduct.harness.referee;

import com.omniproduct.harness.HarnessReport;
import com.omniproduct.harness.Metrics;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

// Un bloc de mesures du commentaire de l'arbitre (CK, PMD) : base, PR, écart et points de chaque mesure. Une mesure qui
// s'améliore rapporte des points (plafond par mesure, et plafond total), une mesure qui se dégrade ne coûte rien.
// Un bloc non noté (scored = false) est affiché pour comparaison. Vide si l'un des deux rapports n'a pas ces mesures.
record MetricTable<M>(String name, String source, Function<Metrics, M> extract, List<Row<M>> rows, int cap, boolean scored) {

    // step : amélioration qui vaut 1 point ; cap : points maximum ; lowerIsBetter : sens de l'amélioration.
    // step = 0 : affiché sans points.
    record Row<M>(String label, String meaning, ToDoubleFunction<M> measure, double step, int cap, boolean lowerIsBetter) {

        int points(M before, M after) {
            double gain = lowerIsBetter
                    ? measure.applyAsDouble(before) - measure.applyAsDouble(after)
                    : measure.applyAsDouble(after) - measure.applyAsDouble(before);
            // Arrondi avant la division : 0.90 - 0.75 doit valoir 0.15, pas 0.1499999.
            return step == 0 ? 0 : (int) Math.min(cap, Math.max(0, Math.floor(Math.round(gain * 1000) / 1000.0 / step)));
        }
    }

    private record Pair<M>(M before, M after) {
    }

    // Points du bloc : 0 s'il n'est pas noté.
    int points(Optional<HarnessReport> base, HarnessReport head) {
        return scored ? raw(base, head) : 0;
    }

    private int raw(Optional<HarnessReport> base, HarnessReport head) {
        return pair(base, head)
                .map(pair -> Math.min(cap, rows.stream().mapToInt(row -> row.points(pair.before(), pair.after())).sum()))
                .orElse(0);
    }

    // counted : faux pour une PR refusée, dont les points ne comptent pas.
    List<String> lines(Optional<HarnessReport> base, HarnessReport head, boolean counted) {
        return pair(base, head).map(pair -> table(pair, raw(base, head), counted)).orElse(List.of());
    }

    private Optional<Pair<M>> pair(Optional<HarnessReport> base, HarnessReport head) {
        return measures(base).flatMap(before -> measures(Optional.of(head)).map(after -> new Pair<>(before, after)));
    }

    private Optional<M> measures(Optional<HarnessReport> report) {
        return report.map(HarnessReport::metrics).map(extract);
    }

    private List<String> table(Pair<M> pair, int total, boolean counted) {
        String summary = !scored ? "shown for comparison, no points"
                : signed(total) + (counted ? " (up to +" + cap + ")" : ", not counted: the PR is not accepted");
        return Stream.of(
                        Stream.of("<details><summary>📐 " + name + " of model/ and service/: " + summary + "</summary>", "",
                                "| Metric | Base | This PR | Δ |" + (scored ? " Points | Rule |" : ""),
                                "|---|---|---|---|" + (scored ? "---|---|" : "")),
                        rows.stream().map(row -> row(row, pair)),
                        Stream.of("", "Measured by " + source + "."
                                + (scored ? " A metric that gets worse costs no points, but read why in Files changed." : ""), "</details>"))
                .flatMap(lines -> lines)
                .toList();
    }

    private String row(Row<M> row, Pair<M> pair) {
        double from = row.measure().applyAsDouble(pair.before());
        double to = row.measure().applyAsDouble(pair.after());
        int points = row.points(pair.before(), pair.after());
        return "| " + row.label() + " | " + format(from) + " | " + format(to) + " | " + delta(to - from) + " |"
                + (scored ? " " + (points == 0 ? "" : signed(points)) + " | " + row.meaning() + " |" : "");
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
