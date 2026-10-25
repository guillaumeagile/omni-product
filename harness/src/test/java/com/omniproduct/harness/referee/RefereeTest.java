package com.omniproduct.harness.referee;

import com.omniproduct.harness.CkMetrics;
import com.omniproduct.harness.HarnessReport;
import com.omniproduct.harness.Metrics;
import com.omniproduct.harness.PmdQuality;
import com.omniproduct.harness.HarnessReport.RuleResult;
import com.omniproduct.harness.referee.Referee.Outcome;
import com.omniproduct.harness.referee.Referee.PullRequest;
import com.omniproduct.harness.referee.Referee.Verdict;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RefereeTest {

    private static RuleResult rule(String id, String status, int violations) {
        return new RuleResult(id, id + " rule", id.equals("HA12.1") ? "WARNING" : "ERROR", status, violations);
    }

    private static Optional<HarnessReport> report(RuleResult... rules) {
        return Optional.of(new HarnessReport("2", "FAIL", List.of(rules), null));
    }

    private static Optional<HarnessReport> reportWith(Metrics metrics, RuleResult... rules) {
        return Optional.of(new HarnessReport("2", "FAIL", List.of(rules), metrics));
    }

    private static PullRequest pr(String title) {
        return new PullRequest(title, "", List.of("src/main/java/com/omniproduct/model/Product.java"), 200, 50);
    }

    private static final Optional<HarnessReport> BASE = report(
            rule("HA7", "PASS", 0), rule("HA9", "FAIL", 2), rule("HA2", "FAIL", 47),
            rule("HA4.3", "FAIL", 1), rule("HA12.1", "WARN", 5));

    @Test
    void acceptsABabyStepAndScoresIt() {
        Verdict verdict = Referee.judge(BASE, report(
                rule("HA7", "PASS", 0), rule("HA9", "PASS", 0), rule("HA2", "FAIL", 47),
                rule("HA4.3", "FAIL", 1), rule("HA12.1", "WARN", 5)), pr("HA9 green: entities implement Identifiable"));

        assertThat(verdict.outcome()).isEqualTo(Outcome.ACCEPTED);
        assertThat(verdict.target()).contains("HA9");
        assertThat(verdict.score()).isEqualTo(Referee.GREEN + Referee.SINGLE_GREEN_BONUS);
        assertThat(verdict.badges()).contains("🍼 Baby step", "🧼 No regression");
        assertThat(verdict.level()).isEqualTo("Single Concern Adept (2/4)");
    }

    @Test
    void rejectsARegressionEvenWhenTheTargetTurnsGreen() {
        Verdict verdict = Referee.judge(BASE, report(
                rule("HA7", "FAIL", 1), rule("HA9", "PASS", 0), rule("HA2", "FAIL", 47),
                rule("HA4.3", "FAIL", 1), rule("HA12.1", "WARN", 5)), pr("HA9 green"));

        assertThat(verdict.outcome()).isEqualTo(Outcome.REJECTED);
        assertThat(verdict.problems()).anySatisfy(problem -> assertThat(problem).contains("`HA7` regressed"));
        assertThat(verdict.score()).isEqualTo(Referee.GREEN + Referee.REGRESSION);
    }

    @Test
    void rejectsATargetThatIsNotGreenYetButCountsTheProgress() {
        Verdict verdict = Referee.judge(BASE, report(
                rule("HA7", "PASS", 0), rule("HA9", "FAIL", 2), rule("HA2", "FAIL", 12),
                rule("HA4.3", "FAIL", 1), rule("HA12.1", "WARN", 5)), pr("HA2: records instead of classes"));

        assertThat(verdict.outcome()).isEqualTo(Outcome.REJECTED);
        assertThat(verdict.problems()).anySatisfy(problem -> assertThat(problem).contains("`HA2` is not green yet"));
        assertThat(verdict.score()).isEqualTo(Referee.PROGRESS_CAP);
    }

    @Test
    void twoGreensAtOnceAreAcceptedWithoutTheBabyStepBonus() {
        Verdict verdict = Referee.judge(BASE, report(
                rule("HA7", "PASS", 0), rule("HA9", "PASS", 0), rule("HA2", "PASS", 0),
                rule("HA4.3", "FAIL", 1), rule("HA12.1", "PASS", 0)), pr("HA9 green"));

        assertThat(verdict.outcome()).isEqualTo(Outcome.ACCEPTED);
        assertThat(verdict.notes()).anySatisfy(note -> assertThat(note).contains("not a baby step anymore"));
        assertThat(verdict.badges()).doesNotContain("🍼 Baby step");
        assertThat(verdict.score()).isEqualTo(2 * Referee.GREEN + Referee.WARNING_RESOLVED);
    }

    @Test
    void readsTheTargetFromTheBranchAndPaysDoubleForABoss() {
        PullRequest pr = new PullRequest("Shelf life", "team-red/step-2/ha4-3-shelf-life", List.of(), 12, 50);

        Verdict verdict = Referee.judge(BASE, report(
                rule("HA7", "PASS", 0), rule("HA9", "FAIL", 2), rule("HA2", "FAIL", 47),
                rule("HA4.3", "PASS", 0), rule("HA12.1", "WARN", 5)), pr);

        assertThat(verdict.target()).contains("HA4.3");
        assertThat(verdict.score()).isEqualTo(Referee.BOSS_GREEN + Referee.SINGLE_GREEN_BONUS + Referee.SMALL_DIFF_BONUS);
        assertThat(verdict.badges()).contains("👑 Boss defeated", "🤏 Small diff");
    }

    @Test
    void rejectsAPullRequestThatTouchesTheHarnessOrDoesNotCompile() {
        PullRequest cheat = new PullRequest("HA9 green", "", List.of("harness/src/main/java/com/omniproduct/harness/Steps.java"), 1, 50);

        Verdict cheating = Referee.judge(BASE, BASE, cheat);
        Verdict broken = Referee.judge(BASE, Optional.empty(), pr("HA9 green"));

        assertThat(cheating.outcome()).isEqualTo(Outcome.REJECTED);
        assertThat(cheating.score()).isZero();
        assertThat(broken.outcome()).isEqualTo(Outcome.REJECTED);
        assertThat(broken.problems()).anySatisfy(problem -> assertThat(problem).contains("does not compile"));
    }

    @Test
    void aPullRequestWithoutTargetIsOffGame() {
        Verdict verdict = Referee.judge(BASE, BASE, pr("Refactor some stuff"));

        assertThat(verdict.outcome()).isEqualTo(Outcome.OFF_GAME);
        assertThat(verdict.notes()).anySatisfy(note -> assertThat(note).contains("No baby step target"));
    }

    @Test
    void anUnknownTargetIsRejected() {
        Verdict verdict = Referee.judge(BASE, BASE, pr("HA42 green"));

        assertThat(verdict.problems()).anySatisfy(problem -> assertThat(problem).contains("`HA42` is not a rule of step 2"));
    }

    @Test
    void theScorecardShowsOnlyTheRulesThatMovedAndEndsWithAMarker() {
        Verdict verdict = Referee.judge(BASE, report(
                rule("HA7", "PASS", 0), rule("HA9", "PASS", 0), rule("HA2", "FAIL", 47),
                rule("HA4.3", "FAIL", 1), rule("HA12.1", "WARN", 5)), pr("HA9 green"));

        String markdown = Scorecard.markdown(verdict);
        List<String> lines = Arrays.asList(markdown.split("\n"));

        assertThat(markdown).contains("✅ Baby step accepted", "| HA9 🎯 | 🔴 FAIL (2) | 🟢 PASS | +10 |", "**Score: +15**");
        assertThat(markdown).doesNotContain("| HA2 ", "| HA7 ");
        assertThat(lines.get(lines.size() - 1))
                .isEqualTo(Scorecard.MARKER + "{\"target\":\"HA9\",\"outcome\":\"ACCEPTED\",\"score\":15,"
                        + "\"level\":\"Single Concern Adept (2/4)\",\"badges\":[\"🍼 Baby step\",\"🧼 No regression\"]} -->");
    }

    @Test
    void removedTokensAndBranchesEarnPointsCappedPerMeasure() {
        Optional<HarnessReport> base = reportWith(new Metrics(3000, 40, 6),
                rule("HA7", "PASS", 0), rule("HA9", "FAIL", 2));
        Verdict verdict = Referee.judge(base, reportWith(new Metrics(2800, 25, 5),
                rule("HA7", "PASS", 0), rule("HA9", "PASS", 0)), pr("HA9 green"));

        assertThat(verdict.outcome()).isEqualTo(Outcome.ACCEPTED);
        // 200 tokens retirés : +2 ; 15 de complexité retirés : +7 (1 point pour 2).
        assertThat(verdict.score()).isEqualTo(Referee.GREEN + 2 + 7 + Referee.SINGLE_GREEN_BONUS);
    }

    @Test
    void tokenAndComplexityRewardsAreCapped() {
        Optional<HarnessReport> base = reportWith(new Metrics(100_000, 200, 9), rule("HA9", "FAIL", 2));
        Verdict verdict = Referee.judge(base, reportWith(new Metrics(0, 0, 0), rule("HA9", "PASS", 0)), pr("HA9 green"));

        assertThat(verdict.score()).isEqualTo(Referee.GREEN + Referee.SINGLE_GREEN_BONUS + Referee.TOKEN_CAP + Referee.COMPLEXITY_CAP);
        assertThat(verdict.notes()).anySatisfy(note -> assertThat(note).contains("Tokens in model/ and service/: 100000 → 0 (-100000)"));
    }

    @Test
    void aRiseInComplexityIsShownButNotPenalized() {
        Optional<HarnessReport> base = reportWith(new Metrics(3000, 40, 6), rule("HA9", "FAIL", 2));
        Verdict verdict = Referee.judge(base, reportWith(new Metrics(3000, 46, 7), rule("HA9", "PASS", 0)), pr("HA9 green"));

        assertThat(verdict.outcome()).isEqualTo(Outcome.ACCEPTED);
        assertThat(verdict.score()).isEqualTo(Referee.GREEN + Referee.SINGLE_GREEN_BONUS);
        assertThat(verdict.notes()).anySatisfy(note -> assertThat(note).contains("Cyclomatic complexity of model/ and service/: 40 → 46 (+6)"));
    }

    private static CkMetrics ck(int classes, int coupling, int maxCoupling, int maxWmc, double maxLcom,
                                int mutableFields, int assignments, int loops, int lambdas, int maxNesting) {
        return new CkMetrics(classes, coupling, maxCoupling, maxWmc, maxLcom, mutableFields, assignments, loops, lambdas, maxNesting);
    }

    // types, fanOut, maxFanOut, maxWmc, minTcc, mutableFields, assignments, loops, lambdas, cognitive, maxCognitive
    private static PmdQuality pmd(int types, int fanOut, int maxFanOut, int maxWmc, double minTcc, int mutableFields,
                                  int assignments, int loops, int lambdas, int cognitive, int maxCognitive) {
        return new PmdQuality(types, fanOut, maxFanOut, maxWmc, minTcc, mutableFields, assignments, loops, lambdas, cognitive, maxCognitive);
    }

    private static final CkMetrics SOME_CK = ck(8, 37, 14, 28, 0.90, 27, 64, 0, 5, 0);

    @Test
    void pmdImprovementsEarnPointsButTheNumberOfTypesDoesNot() {
        Optional<HarnessReport> base = reportWith(new Metrics(3000, 40, 6, SOME_CK, pmd(10, 59, 18, 28, 0.0, 27, 63, 0, 5, 2, 1)),
                rule("HA9", "FAIL", 2));
        Verdict verdict = Referee.judge(base, reportWith(new Metrics(3000, 40, 6, SOME_CK, pmd(16, 70, 14, 13, 0.25, 20, 63, 0, 5, 2, 1)),
                rule("HA9", "PASS", 0)), pr("HA9 green"));

        // Fan-out max -4 : +2 ; WMC max -15 : +3 (plafond) ; TCC +0.25 : +2 (plafond) ; champs mutables -7 : +3 (plafond).
        // +6 types et +11 de fan-out total : 0.
        assertThat(verdict.score()).isEqualTo(Referee.GREEN + Referee.SINGLE_GREEN_BONUS + MetricTables.CAP);
        assertThat(Scorecard.markdown(verdict)).contains("PMD quality metrics of model/ and service/: +10 (up to +10)",
                "| Types (classes, records, enums) | 10 | 16 | +6 |  | not scored",
                "| WMC max (class complexity) | 28 | 13 | -15 | +3 |",
                "| TCC min (cohesion, 0..1) | 0 | 0.25 | +0.25 | +2 |");
    }

    @Test
    void ckIsShownForComparisonButEarnsNothing() {
        Optional<HarnessReport> base = reportWith(new Metrics(3000, 40, 6, SOME_CK), rule("HA9", "FAIL", 2));
        Verdict verdict = Referee.judge(base, reportWith(new Metrics(3000, 40, 6, ck(1, 0, 0, 0, 0, 0, 0, 0, 50, 0)),
                rule("HA9", "PASS", 0)), pr("HA9 green"));

        assertThat(verdict.score()).isEqualTo(Referee.GREEN + Referee.SINGLE_GREEN_BONUS);
        assertThat(Scorecard.markdown(verdict)).contains("CK metrics of model/ and service/: shown for comparison, no points",
                "| CBO total (coupling) | 37 | 0 | -37 |");
    }

    @Test
    void pmdPointsAreCappedAndAWorseMetricCostsNothing() {
        Optional<HarnessReport> base = reportWith(new Metrics(3000, 40, 6, null, pmd(1, 500, 50, 100, 0, 100, 500, 50, 0, 90, 30)),
                rule("HA9", "FAIL", 2));
        Verdict best = Referee.judge(base, reportWith(new Metrics(3000, 40, 6, null, pmd(99, 0, 0, 0, 1, 0, 0, 0, 50, 0, 0)),
                rule("HA9", "PASS", 0)), pr("HA9 green"));
        Verdict worse = Referee.judge(base, reportWith(new Metrics(3000, 40, 6, null, pmd(1, 900, 90, 200, 0, 200, 900, 90, 0, 99, 40)),
                rule("HA9", "PASS", 0)), pr("HA9 green"));

        assertThat(best.score()).isEqualTo(Referee.GREEN + Referee.SINGLE_GREEN_BONUS + MetricTables.CAP);
        assertThat(worse.score()).isEqualTo(Referee.GREEN + Referee.SINGLE_GREEN_BONUS);
    }

    @Test
    void aRejectedPullRequestEarnsNoPmdPoints() {
        Optional<HarnessReport> base = reportWith(new Metrics(3000, 40, 6, null, pmd(10, 59, 18, 28, 0.0, 27, 63, 0, 5, 2, 1)),
                rule("HA9", "FAIL", 2));
        Verdict verdict = Referee.judge(base, reportWith(new Metrics(3000, 40, 6, null, pmd(10, 0, 0, 0, 1, 0, 0, 0, 5, 0, 0)),
                rule("HA9", "FAIL", 1)), pr("HA9 green"));

        assertThat(verdict.outcome()).isEqualTo(Outcome.REJECTED);
        assertThat(verdict.score()).isEqualTo(1);
        assertThat(Scorecard.markdown(verdict)).contains("PMD quality metrics of model/ and service/: +10, not counted: the PR is not accepted");
    }

    @Test
    void theScorecardRecapsWhereEachPointComesFrom() {
        Optional<HarnessReport> base = reportWith(new Metrics(3000, 40, 6, ck(8, 37, 14, 28, 0.90, 27, 64, 0, 5, 0)),
                rule("HA7", "PASS", 0), rule("HA9", "FAIL", 2), rule("HA11", "FAIL", 10), rule("HA12.1", "WARN", 3));
        Verdict verdict = Referee.judge(base, reportWith(new Metrics(2800, 30, 6, ck(13, 59, 11, 13, 0.75, 32, 75, 0, 5, 0)),
                rule("HA7", "FAIL", 1), rule("HA9", "PASS", 0), rule("HA11", "FAIL", 8), rule("HA12.1", "PASS", 0)),
                new PullRequest("HA9 green", "", List.of(), 120, 400));

        String markdown = Scorecard.markdown(verdict);

        assertThat(verdict.breakdown().stream().mapToInt(Referee.Points::points).sum()).isEqualTo(verdict.score());
        assertThat(markdown).contains(
                "| Score | Points |",
                "| `HA9` turned green 🎯 | +10 |",
                "| `HA7` regressed (was green) | -5 |",
                "| `HA12.1` warning resolved | +3 |",
                "| Violations removed on rules still red (up to +5) | +2 |",
                "| Tokens removed (+1 per 100, up to +5) | +2 |",
                "| Cyclomatic complexity removed (+1 per 2, up to +10) | +5 |",
                "| **Total** | **" + "+" + verdict.score() + "** |");
        // Refusée (régression de HA7) : ni bonus de baby step ou de petit diff, ni points de mesures.
        assertThat(markdown).doesNotContain("Baby step: exactly one", "Small diff:", "quality metrics improved");
        assertThat(markdown).contains("CK metrics of model/ and service/: shown for comparison, no points");
    }
}
