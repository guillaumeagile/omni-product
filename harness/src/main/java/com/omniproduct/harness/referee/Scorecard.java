package com.omniproduct.harness.referee;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniproduct.harness.referee.Referee.Outcome;
import com.omniproduct.harness.referee.Referee.RuleChange;
import com.omniproduct.harness.referee.Referee.Verdict;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
                : "**Score: " + signed(verdict.score()) + "** · Level: " + verdict.level();

        return Stream.of(
                        Stream.of(title, "", outcome, ""),
                        problems.stream(),
                        notes.stream(),
                        problems.isEmpty() && notes.isEmpty() ? Stream.<String>empty() : Stream.of(""),
                        table.stream(),
                        table.isEmpty() ? Stream.<String>empty() : Stream.of(""),
                        breakdown(verdict).stream(),
                        score.isEmpty() ? Stream.<String>empty() : Stream.of(score, ""),
                        badges(verdict).stream(),
                        verdict.measures().stream(),
                        verdict.measures().isEmpty() ? Stream.<String>empty() : Stream.of(""),
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

    // Les badges en grand, en images shields.io (style for-the-badge) ; le texte alternatif garde le nom du badge.
    // La même table de couleurs est dans le workflow (section de la description de la PR).
    static final Map<String, String> BADGE_COLORS = Map.of(
            "🍼 Baby step", "2ea44f",
            "🧼 No regression", "1f6feb",
            "🤏 Small diff", "8250df",
            "🧹 Clean Coder", "0e8a8a",
            "👑 Boss defeated", "d4a017",
            "🧠 Compiler-checked", "e36209");

    private static List<String> badges(Verdict verdict) {
        return verdict.outcome() == Outcome.OFF_GAME || verdict.badges().isEmpty() ? List.of()
                : List.of(verdict.badges().stream().map(Scorecard::shield).collect(Collectors.joining(" ")), "");
    }

    // Dans un chemin shields.io, « - » s'écrit « -- » et « _ » s'écrit « __ ».
    static String shield(String badge) {
        String text = URLEncoder.encode(badge.replace("-", "--").replace("_", "__"), StandardCharsets.UTF_8).replace("+", "%20");
        return "![" + badge + "](https://img.shields.io/badge/" + text + "-" + BADGE_COLORS.getOrDefault(badge, "555555")
                + "?style=for-the-badge)";
    }

    // Le récapitulatif du score : une ligne par source de points, puis le total.
    private static List<String> breakdown(Verdict verdict) {
        return verdict.outcome() == Outcome.OFF_GAME || verdict.breakdown().isEmpty() ? List.of()
                : Stream.of(
                                Stream.of("| Score | Points |", "|---|---:|"),
                                verdict.breakdown().stream().map(line -> "| " + line.reason() + " | " + signed(line.points()) + " |"),
                                Stream.of("| **Total** | **" + signed(verdict.score()) + "** |", ""))
                        .flatMap(lines -> lines)
                        .toList();
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
