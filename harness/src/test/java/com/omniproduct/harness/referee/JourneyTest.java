package com.omniproduct.harness.referee;

import com.omniproduct.harness.HarnessReport;
import com.omniproduct.harness.HarnessReport.RuleResult;
import com.omniproduct.harness.Metrics;
import com.omniproduct.harness.referee.Referee.PullRequest;
import com.omniproduct.harness.referee.Referee.Verdict;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JourneyTest {

    private static RuleResult rule(String id, String status, int violations) {
        return new RuleResult(id, id + " rule", "ERROR", status, violations);
    }

    private static HarnessReport report(long tokens, RuleResult... rules) {
        return new HarnessReport("1", "FAIL", List.of(rules), new Metrics(tokens, 40, 6));
    }

    // Départ de la phase : HA8 et HA9 rouges. Branche précédente (HA8) : HA8 vert. Cette PR (HA9) : les deux verts.
    private static final HarnessReport PHASE = report(3000, rule("HA8", "FAIL", 30), rule("HA9", "FAIL", 5), rule("HA10", "FAIL", 1));
    private static final HarnessReport PREVIOUS = report(2900, rule("HA8", "PASS", 0), rule("HA9", "FAIL", 5), rule("HA10", "FAIL", 1));
    private static final HarnessReport HEAD = report(2800, rule("HA8", "PASS", 0), rule("HA9", "PASS", 0), rule("HA10", "FAIL", 1));

    private static final PullRequest PR = new PullRequest("HA9 green: Identifiable", "team-red/phase-1/ha9-identifiable",
            List.of("src/main/java/com/omniproduct/model/Product.java"), 120, 400);

    @Test
    void theRoundIsScoredAgainstThePreviousBranchSoTheBabyStepCountsOnce() {
        Verdict verdict = Referee.judge(Optional.of(PREVIOUS), Optional.of(HEAD), PR)
                .withContext(Journey.context("`team-red/phase-1/ha8-no-jpa` (abc1234)"), Journey.lines(Optional.of(PHASE), HEAD));

        String markdown = Scorecard.markdown(verdict);

        // HA8 était déjà vert au branche précédente : seul HA9 rapporte, avec le bonus de baby step.
        assertThat(verdict.score()).isEqualTo(Referee.GREEN + Referee.SINGLE_GREEN_BONUS + Referee.SMALL_DIFF_BONUS + 1);
        assertThat(verdict.badges()).contains("🍼 Baby step");
        assertThat(markdown).contains(
                "Round scored against your previous branch `team-red/phase-1/ha8-no-jpa` (abc1234)",
                "🧭 Since the start of the phase: 2 rule(s) turned green · Level Legacy Survivor (0/3) → Composable Craftsman (2/3)",
                "| HA8 | 🔴 FAIL (30) | 🟢 PASS |",
                "| HA9 | 🔴 FAIL (5) | 🟢 PASS |",
                "- Tokens in model/ and service/: 3000 → 2800");
        assertThat(markdown).doesNotContain("| HA10 |", "rules turned green at once");
        // Les badges en grand, en images shields.io.
        assertThat(markdown).contains(
                "![🍼 Baby step](https://img.shields.io/badge/%F0%9F%8D%BC%20Baby%20step-2ea44f?style=for-the-badge)",
                "![🤏 Small diff](https://img.shields.io/badge/%F0%9F%A4%8F%20Small%20diff-8250df?style=for-the-badge)");
        assertThat(Scorecard.shield("🧠 Compiler-checked")).contains("/badge/%F0%9F%A7%A0%20Compiler--checked-e36209?");
    }

    @Test
    void theFirstBranchOfThePhaseHasNoJourneyBlock() {
        Verdict verdict = Referee.judge(Optional.of(PHASE), Optional.of(PREVIOUS), PR)
                .withContext(Journey.context(""), Journey.lines(Optional.empty(), PREVIOUS));

        assertThat(Scorecard.markdown(verdict)).doesNotContain("Since the start of the phase", "previous branch");
    }

    @Test
    void refereeMainReadsThePhaseAndThePreviousBranch(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        PHASE.write(dir.resolve("phase.json"));
        PREVIOUS.write(dir.resolve("previous.json"));
        HEAD.write(dir.resolve("head.json"));

        int exit = RefereeMain.run(new String[]{"--base", dir.resolve("previous.json").toString(),
                "--head", dir.resolve("head.json").toString(), "--title", PR.title(), "--branch", PR.branch(),
                "--phase", dir.resolve("phase.json").toString(), "--previous", "`team-red/phase-1/ha8-no-jpa` (abc1234)",
                "--out", dir.resolve("scorecard.md").toString()});

        assertThat(exit).isZero();
        assertThat(java.nio.file.Files.readString(dir.resolve("scorecard.md")))
                .contains("previous branch `team-red/phase-1/ha8-no-jpa`", "🧭 Since the start of the phase: 2 rule(s)");
    }
}
