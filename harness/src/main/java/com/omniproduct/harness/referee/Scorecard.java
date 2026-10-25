package com.omniproduct.harness.referee;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniproduct.harness.referee.Referee.Outcome;
import com.omniproduct.harness.referee.Referee.RuleChange;
import com.omniproduct.harness.referee.Referee.Verdict;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Le commentaire de l'arbitre sur la PR : verdict, règles qui ont bougé, score, niveau, badges. La dernière ligne est
// un marqueur HTML invisible (JSON) que le classement relit, pour ne garder aucun état caché.
public final class Scorecard {

    public static final String MARKER = "<!-- harness-referee ";

    private static final ObjectMapper JSON = new ObjectMapper();

    private Scorecard() {
    }

    public static String markdown(Verdict verdict) {
        String title = "## 🎮 Arbitre CUPID" + verdict.target().map(target -> " — " + target).orElse("");
        String outcome = switch (verdict.outcome()) {
            case ACCEPTED -> "**✅ Baby step accepted**";
            case REJECTED -> "**❌ Not accepted**";
            case OFF_GAME -> "**⚪ Off game**";
        };
        List<String> problems = verdict.problems().stream().map(problem -> "- ❌ " + problem).toList();
        List<String> notes = verdict.notes().stream().map(note -> "- ℹ️ " + note).toList();
        List<String> table = table(verdict.changes());
        String score = verdict.outcome() == Outcome.OFF_GAME ? ""
                : "**Score: " + signed(verdict.score()) + "** · Level: " + verdict.level()
                + (verdict.badges().isEmpty() ? "" : " · Badges: " + String.join(", ", verdict.badges()));

        return Stream.of(
                        Stream.of(title, "", outcome, ""),
                        problems.stream(),
                        notes.stream(),
                        problems.isEmpty() && notes.isEmpty() ? Stream.<String>empty() : Stream.of(""),
                        table.stream(),
                        table.isEmpty() ? Stream.<String>empty() : Stream.of(""),
                        score.isEmpty() ? Stream.<String>empty() : Stream.of(score, ""),
                        Stream.of(marker(verdict)))
                .flatMap(lines -> lines)
                .collect(Collectors.joining("\n", "", "\n"));
    }

    // Seules les règles qui ont bougé (et la cible) : le tableau montre ce que la PR a changé.
    private static List<String> table(List<RuleChange> changes) {
        List<String> rows = changes.stream()
                .filter(RuleChange::changed)
                .map(rule -> "| " + rule.id() + (rule.target() ? " 🎯" : "") + " | " + Referee.describe(rule.base())
                        + " | " + Referee.describe(java.util.Optional.of(rule.head())) + " | "
                        + (rule.points() == 0 ? "" : signed(rule.points())) + " |")
                .toList();
        return rows.isEmpty() ? List.of()
                : Stream.concat(Stream.of("| Rule | Base | This PR | Points |", "|---|---|---|---|"), rows.stream()).toList();
    }

    // Ce que le classement relit dans chaque commentaire de l'arbitre.
    public record Marker(String target, String outcome, int score, String level, List<String> badges) {
    }

    private static String marker(Verdict verdict) {
        Marker data = new Marker(verdict.target().orElse(""), verdict.outcome().name(), verdict.score(), verdict.level(), verdict.badges());
        try {
            return MARKER + JSON.writeValueAsString(data) + " -->";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String signed(int points) {
        return points > 0 ? "+" + points : String.valueOf(points);
    }
}
