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
// s'améliore rapporte des points (plafond par mesure, pas de plafond total : c'est du bonus), une mesure qui se dégrade
// ne coûte rien.
// Un bloc non noté (scored = false) est affiché pour comparaison. Vide si l'un des deux rapports n'a pas ces mesures.
record MetricTable<M>(String name, String source, Function<Metrics, M> extract, List<Row<M>> rows, boolean scored) {

    // step : amélioration qui vaut 1 point ; cap : points maximum ; lowerIsBetter : sens de l'amélioration.
    // step = 0 : affiché sans points. penaltyStep : dégradation qui coûte 1 point, sans plafond ; 0 : la dégradation
    // ne coûte rien.
    record Row<M>(String label, String meaning, ToDoubleFunction<M> measure, double step, int cap, boolean lowerIsBetter,
                  double penaltyStep) {

        Row(String label, String meaning, ToDoubleFunction<M> measure, double step, int cap, boolean lowerIsBetter) {
            this(label, meaning, measure, step, cap, lowerIsBetter, 0);
        }

        // Arrondi avant la division : 0.90 - 0.75 doit valoir 0.15, pas 0.1499999.
        private double gain(M before, M after) {
            double gain = lowerIsBetter
                    ? measure.applyAsDouble(before) - measure.applyAsDouble(after)
                    : measure.applyAsDouble(after) - measure.applyAsDouble(before);
            return Math.round(gain * 1000) / 1000.0;
        }

        int reward(M before, M after) {
            return step == 0 ? 0 : (int) Math.min(cap, Math.max(0, Math.floor(gain(before, after) / step)));
        }

        int penalty(M before, M after) {
            return penaltyStep == 0 ? 0 : (int) Math.max(0, Math.floor(-gain(before, after) / penaltyStep));
        }
    }

    private record Pair<M>(M before, M after) {
    }

    // Points du bloc : seules les pénalités d'un bloc non noté.
    int points(Optional<HarnessReport> base, HarnessReport head) {
        return raw(base, head);
    }

    private int raw(Optional<HarnessReport> base, HarnessReport head) {
        return pair(base, head)
                .map(pair -> rows.stream().mapToInt(row -> rowPoints(row, pair)).sum())
                .orElse(0);
    }

    private int rowPoints(Row<M> row, Pair<M> pair) {
        return scored ? row.reward(pair.before(), pair.after()) - row.penalty(pair.before(), pair.after())
                : -row.penalty(pair.before(), pair.after());
    }

    // Les colonnes de points : bloc noté, ou bloc non noté dont une ligne peut coûter des points.
    private boolean showsPoints() {
        return scored || rows.stream().anyMatch(row -> row.penaltyStep() > 0);
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
        String notCounted = counted ? "" : ", not counted: the PR is not accepted";
        String summary = scored ? signed(total) + notCounted
                : showsPoints() ? "shown for comparison, only a worse CBO total costs points (-1 per 5 added): " + signed(total) + notCounted
                : "shown for comparison, no points";
        return Stream.of(
                        Stream.of("<details><summary>📐 " + name + " of model/ and service/: " + summary + "</summary>", "",
                                "| Metric | Base | This PR | Δ |" + (showsPoints() ? " Points | Rule |" : ""),
                                "|---|---|---|---|" + (showsPoints() ? "---|---|" : "")),
                        rows.stream().map(row -> row(row, pair)),
                        Stream.of("", "Measured by " + source + "."
                                + (scored ? " A metric that gets worse costs no points, but read why in Files changed." : ""), "</details>"))
                .flatMap(lines -> lines)
                .toList();
    }

    private String row(Row<M> row, Pair<M> pair) {
        double from = row.measure().applyAsDouble(pair.before());
        double to = row.measure().applyAsDouble(pair.after());
        int points = rowPoints(row, pair);
        return "| " + row.label() + " | " + format(from) + " | " + format(to) + " | " + delta(to - from) + " |"
                + (showsPoints() ? " " + (points == 0 ? "" : signed(points)) + " | " + row.meaning() + " |" : "");
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
