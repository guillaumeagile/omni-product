package com.omniproduct.harness.referee;

import com.omniproduct.harness.CkMetrics;
import com.omniproduct.harness.Metrics;
import com.omniproduct.harness.PmdQuality;
import com.omniproduct.harness.referee.MetricTable.Row;

import java.util.List;

// Les blocs de mesures de l'arbitre en phase 2 (essai testPMD). PMD est noté : il lit Java 21, records compris.
// CK est affiché pour comparaison, sans points : CK 0.7.0 ne voit pas les records, qu'il ferait disparaître des
// mesures (points gagnés sans rien améliorer). Le nombre de types est affiché sans points : découper n'est pas un but.
final class MetricTables {

    static final int CAP = 10;

    static final MetricTable<PmdQuality> PMD = new MetricTable<>("PMD quality metrics",
            "[PMD](https://pmd.github.io/) 7.7.0 metrics (WMC, TCC, FAN_OUT, cognitive complexity)", Metrics::quality, List.of(
            new Row<>("Types (classes, records, enums)", "not scored: splitting is not a goal in itself", PmdQuality::types, 0, 0, true),
            new Row<>("Fan-out total (coupling)", "+1 per 5 removed, up to +3", PmdQuality::fanOut, 5, 3, true),
            new Row<>("Fan-out max", "+1 per 2 removed, up to +2", PmdQuality::maxFanOut, 2, 2, true),
            new Row<>("WMC max (class complexity)", "+1 per 3 removed, up to +3", PmdQuality::maxWmc, 3, 3, true),
            new Row<>("TCC min (cohesion, 0..1)", "+1 per 0.1 gained, up to +2", PmdQuality::minTcc, 0.1, 2, false),
            new Row<>("Mutable fields (non final)", "+1 per 2 removed, up to +3", PmdQuality::mutableFields, 2, 3, true),
            new Row<>("Assignments", "+1 per 5 removed, up to +2", PmdQuality::assignments, 5, 2, true),
            new Row<>("Loops", "+1 per loop removed, up to +2", PmdQuality::loops, 1, 2, true),
            new Row<>("Lambdas", "+1 per 2 added, up to +1", PmdQuality::lambdas, 2, 1, false),
            new Row<>("Cognitive complexity total", "+1 per 3 removed, up to +2", PmdQuality::cognitive, 3, 2, true),
            new Row<>("Cognitive complexity max", "+1 per 2 removed, up to +2", PmdQuality::maxCognitive, 2, 2, true)),
            CAP, true);

    static final MetricTable<CkMetrics> CK = new MetricTable<>("CK metrics",
            "[CK](https://github.com/mauricioaniche/ck) 0.7.0, which does not see records", Metrics::ck, List.of(
            new Row<>("Classes", "", CkMetrics::classes, 0, 0, true),
            new Row<>("CBO total (coupling)", "", CkMetrics::coupling, 0, 0, true),
            new Row<>("CBO max", "", CkMetrics::maxCoupling, 0, 0, true),
            new Row<>("WMC max (class complexity)", "", CkMetrics::maxWmc, 0, 0, true),
            new Row<>("LCOM* max (lack of cohesion, 0..1)", "", CkMetrics::maxLcom, 0, 0, true),
            new Row<>("Mutable fields (non final)", "", CkMetrics::mutableFields, 0, 0, true),
            new Row<>("Assignments", "", CkMetrics::assignments, 0, 0, true),
            new Row<>("Loops", "", CkMetrics::loops, 0, 0, true),
            new Row<>("Lambdas", "", CkMetrics::lambdas, 0, 0, true),
            new Row<>("Max nesting", "", CkMetrics::maxNesting, 0, 0, true)),
            0, false);

    static final List<MetricTable<?>> TABLES = List.of(PMD, CK);

    private MetricTables() {
    }
}
