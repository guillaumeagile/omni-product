package com.omniproduct.harness.referee;

import com.omniproduct.harness.HarnessReport;
import com.omniproduct.harness.HarnessReport.RuleResult;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// L'arbitre des PR de l'atelier : compare le rapport du harnais sur la base de la PR et sur sa tête, et dit si la PR
// est un baby step (une règle cible passée au vert, sans régression, sans toucher au harnais), avec un score.
// Fonction pure : mêmes rapports et même PR en entrée, même verdict en sortie.
public final class Referee {

    // HA9, ha 4.3, HA4-3 (nom de branche), HA12_1 : la cible du baby step.
    private static final Pattern TARGET = Pattern.compile("(?i)\\bha\\s*(\\d+)(?:[.\\-_](\\d+))?");
    private static final Set<String> BOSSES = Set.of("HA4.3", "HA4.5");
    private static final String PASS = "PASS";
    private static final String FAIL = "FAIL";
    private static final String WARN = "WARN";

    static final int GREEN = 10;
    static final int BOSS_GREEN = 20;
    static final int WARNING_RESOLVED = 3;
    static final int PROGRESS_CAP = 5;
    static final int SINGLE_GREEN_BONUS = 5;
    static final int SMALL_DIFF_BONUS = 3;
    static final int REGRESSION = -5;

    private Referee() {
    }

    // Ce que l'Action sait de la PR : titre, branche, fichiers modifiés, lignes modifiées hors tests.
    public record PullRequest(String title, String branch, List<String> changedFiles, int changedLines, int smallDiff) {
    }

    // Une règle vue des deux côtés. base vide : la règle n'existait pas dans le rapport de la base.
    public record RuleChange(String id, String severity, Optional<RuleResult> base, RuleResult head, boolean target, int points) {

        boolean turnedGreen() {
            return head.status().equals(PASS) && base.map(rule -> !rule.status().equals(PASS)).orElse(false);
        }

        boolean regressed() {
            return head.status().equals(FAIL) && base.map(rule -> rule.status().equals(PASS)).orElse(false);
        }

        boolean changed() {
            return target || base.map(rule -> !rule.status().equals(head.status()) || rule.violations() != head.violations())
                    .orElse(true);
        }
    }

    public enum Outcome { ACCEPTED, REJECTED, OFF_GAME }

    public record Verdict(Outcome outcome, Optional<String> target, List<RuleChange> changes, List<String> problems,
                          List<String> notes, int score, String level, List<String> badges) {
    }

    public static Optional<String> target(PullRequest pr) {
        return Stream.of(pr.title(), pr.branch())
                .map(TARGET::matcher)
                .filter(Matcher::find)
                .findFirst()
                .map(match -> "HA" + match.group(1) + Optional.ofNullable(match.group(2)).map(minor -> "." + minor).orElse(""));
    }

    public static Verdict judge(Optional<HarnessReport> base, Optional<HarnessReport> head, PullRequest pr) {
        Optional<String> target = target(pr);
        boolean touchesHarness = pr.changedFiles().stream().anyMatch(file -> file.startsWith("harness/"));

        if (head.isEmpty()) {
            return rejected(target, List.of(), List.of("The harness could not run on this PR: the project does not compile."));
        }
        if (touchesHarness) {
            return rejected(target, List.of(), List.of("This PR modifies `harness/`: the rules are the referee, not part of the game."));
        }
        if (target.isEmpty()) {
            return new Verdict(Outcome.OFF_GAME, target, List.of(), List.of(),
                    List.of("No baby step target found: put the rule in the PR title (`HA9 green: ...`) or in the branch name (`step-1/ha9-...`)."),
                    0, level(head.get()), List.of());
        }

        List<RuleChange> changes = changes(base, head.get(), target.get());
        Optional<RuleChange> targeted = changes.stream().filter(RuleChange::target).findFirst();
        List<RuleChange> regressions = changes.stream().filter(RuleChange::regressed).toList();
        long greens = changes.stream().filter(RuleChange::turnedGreen).count();

        List<String> problems = Stream.of(
                        targeted.isEmpty()
                                ? Stream.of("`" + target.get() + "` is not a rule of step " + head.get().step() + ".")
                                : Stream.<String>empty(),
                        targeted.filter(rule -> !rule.turnedGreen()).stream()
                                .map(rule -> "`" + rule.id() + "` is not green yet (" + describe(Optional.of(rule.head())) + ")."),
                        regressions.stream().map(rule -> "`" + rule.id() + "` regressed: it was green and now fails."))
                .flatMap(lines -> lines)
                .toList();

        boolean accepted = problems.isEmpty();
        boolean babyStep = accepted && greens == 1;
        boolean smallDiff = accepted && pr.changedLines() <= pr.smallDiff();

        List<String> notes = Stream.of(
                        accepted && greens > 1
                                ? Stream.of(greens + " rules turned green at once: that is not a baby step anymore, no baby-step bonus.")
                                : Stream.<String>empty(),
                        smallDiff ? Stream.of("Small diff: " + pr.changedLines() + " changed lines (limit " + pr.smallDiff() + ").")
                                : Stream.<String>empty())
                .flatMap(lines -> lines)
                .toList();

        int score = changes.stream().mapToInt(RuleChange::points).sum()
                + progress(changes)
                + (babyStep ? SINGLE_GREEN_BONUS : 0)
                + (smallDiff ? SMALL_DIFF_BONUS : 0);

        List<String> badges = Stream.of(
                        babyStep ? Optional.of("🍼 Baby step") : Optional.<String>empty(),
                        accepted && regressions.isEmpty() ? Optional.of("🧼 No regression") : Optional.<String>empty(),
                        smallDiff ? Optional.of("🤏 Small diff") : Optional.<String>empty(),
                        changes.stream().anyMatch(rule -> rule.turnedGreen() && BOSSES.contains(rule.id()))
                                ? Optional.of("👑 Boss defeated") : Optional.<String>empty(),
                        changes.stream().anyMatch(rule -> rule.turnedGreen() && rule.id().equals("HA4.5"))
                                ? Optional.of("🧠 Compiler-checked") : Optional.<String>empty())
                .flatMap(Optional::stream)
                .toList();

        return new Verdict(accepted ? Outcome.ACCEPTED : Outcome.REJECTED, target, changes, problems, notes,
                score, level(head.get()), badges);
    }

    private static Verdict rejected(Optional<String> target, List<RuleChange> changes, List<String> problems) {
        return new Verdict(Outcome.REJECTED, target, changes, problems, List.of(), 0, "-", List.of());
    }

    private static List<RuleChange> changes(Optional<HarnessReport> base, HarnessReport head, String target) {
        return head.rules().stream()
                .map(rule -> change(base.flatMap(report -> report.rule(rule.id())), rule, rule.id().equalsIgnoreCase(target)))
                .toList();
    }

    private static RuleChange change(Optional<RuleResult> base, RuleResult head, boolean target) {
        RuleChange unscored = new RuleChange(head.id(), head.severity(), base, head, target, 0);
        int points = unscored.turnedGreen()
                ? base.filter(rule -> rule.status().equals(WARN)).map(rule -> WARNING_RESOLVED)
                        .orElse(BOSSES.contains(head.id()) ? BOSS_GREEN : GREEN)
                : unscored.regressed() ? REGRESSION : 0;
        return new RuleChange(head.id(), head.severity(), base, head, target, points);
    }

    // +1 par violation retirée sur une règle encore rouge, plafonné : le progrès compte, sans remplacer le vert.
    private static int progress(List<RuleChange> changes) {
        int removed = changes.stream()
                .filter(rule -> rule.head().status().equals(FAIL))
                .flatMap(rule -> rule.base().filter(base -> base.status().equals(FAIL)).stream()
                        .map(base -> Math.max(0, base.violations() - rule.head().violations())))
                .mapToInt(Integer::intValue)
                .sum();
        return Math.min(PROGRESS_CAP, removed);
    }

    // Niveau d'après les règles bloquantes au vert dans la tête de la PR.
    static String level(HarnessReport head) {
        List<RuleResult> errors = head.rules().stream().filter(rule -> rule.severity().equals("ERROR")).toList();
        long green = errors.stream().filter(rule -> rule.status().equals(PASS)).count();
        double ratio = errors.isEmpty() ? 0 : (double) green / errors.size();
        String name = ratio == 0 ? "Legacy Survivor"
                : ratio < 1.0 / 3 ? "Unix Apprentice"
                : ratio < 2.0 / 3 ? "Single Concern Adept"
                : ratio < 1 ? "Composable Craftsman"
                : "CUPID Master";
        return name + " (" + green + "/" + errors.size() + ")";
    }

    static String describe(Optional<RuleResult> rule) {
        return rule.map(result -> switch (result.status()) {
            case PASS -> "🟢 PASS";
            case WARN -> "🟡 WARN (" + result.violations() + ")";
            default -> "🔴 FAIL (" + result.violations() + ")";
        }).orElse("—");
    }
}
