package com.omniproduct.harness.referee;

import com.omniproduct.harness.HarnessReport;
import com.omniproduct.harness.HarnessReport.RuleResult;
import com.omniproduct.harness.Metrics;

import java.util.List;
import java.util.Optional;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

// Le chemin parcouru depuis le départ de la phase, quand le round est noté par rapport au baby step précédent :
// les branches d'une équipe s'enchaînent et ne sont jamais fusionnées, chaque PR vise la branche de la phase.
// Le score du round reste le delta avec la branche précédente (rien n'est compté deux fois) ; ce bloc montre le cumul,
// sans points. Vide sans rapport du départ de la phase (première branche : le round est déjà mesuré depuis la phase).
final class Journey {

    private Journey() {
    }

    // La note en tête du commentaire : à quoi le round est comparé.
    static List<String> context(String previous) {
        return previous.isBlank() ? List.of()
                : List.of("Round scored against your previous branch " + previous + ": only what this PR adds earns points.");
    }

    static List<String> lines(Optional<HarnessReport> phase, HarnessReport head) {
        return phase.map(start -> table(start, head)).orElse(List.of());
    }

    private static List<String> table(HarnessReport start, HarnessReport head) {
        List<RuleResult> moved = head.rules().stream()
                .filter(rule -> start.rule(rule.id()).map(before -> !before.status().equals(rule.status())).orElse(true))
                .toList();
        long greens = moved.stream()
                .filter(rule -> rule.status().equals("PASS"))
                .filter(rule -> start.rule(rule.id()).map(before -> !before.status().equals("PASS")).orElse(false))
                .count();
        return Stream.of(
                        Stream.of("<details><summary>🧭 Since the start of the phase: " + greens + " rule(s) turned green · Level "
                                + Referee.level(start) + " → " + Referee.level(head) + "</summary>", ""),
                        moved.isEmpty() ? Stream.<String>empty()
                                : Stream.concat(Stream.of("| Rule | Start of the phase | This PR |", "|---|---|---|"),
                                        moved.stream().map(rule -> "| " + rule.id() + " | " + Referee.describe(start.rule(rule.id()))
                                                + " | " + Referee.describe(Optional.of(rule)) + " |")),
                        moved.isEmpty() ? Stream.<String>empty() : Stream.of(""),
                        measured("Tokens in model/ and service/", start, head, Metrics::tokens),
                        measured("Cyclomatic complexity of model/ and service/", start, head, Metrics::complexity),
                        Stream.of("", "No points here: each round earns its own, and the leaderboard adds them up.", "</details>"))
                .flatMap(lines -> lines)
                .toList();
    }

    private static Stream<String> measured(String label, HarnessReport start, HarnessReport head, ToLongFunction<Metrics> measure) {
        return Optional.ofNullable(start.metrics())
                .flatMap(before -> Optional.ofNullable(head.metrics())
                        .map(after -> "- " + label + ": " + measure.applyAsLong(before) + " → " + measure.applyAsLong(after)))
                .stream();
    }
}
