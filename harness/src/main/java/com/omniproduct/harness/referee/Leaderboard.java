package com.omniproduct.harness.referee;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// Le classement de l'atelier, recalculé après chaque round à partir des PR ouvertes de la phase (rien n'est jamais
// fusionné : les PR restent en draft). Pour chaque PR, le dernier commentaire de l'arbitre (posté par le bot, jamais un
// commentaire d'étudiant qui imiterait le marqueur) donne son score, qui est le delta avec le step précédent : la somme
// par équipe est donc le cumul, sans double compte. Une équipe est le premier segment du nom de branche
// (team-red/step-1/ha9 -> team-red). Aucun état caché : relancer donne le même tableau.
//   leaderboard --prs prs.json [--out leaderboard.md]
//   (prs.json : gh pr list --state open --base java-bdxio-phase1 --json number,headRefName,comments)
public final class Leaderboard {

    private static final ObjectMapper JSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final List<String> MEDALS = List.of("🥇", "🥈", "🥉");

    private Leaderboard() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(String login) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Comment(Author author, String body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequest(int number, String headRefName, List<Comment> comments) {
    }

    record Play(String team, int number, Scorecard.Marker marker) {
    }

    record Standing(String team, int score, long babySteps, String level, Map<String, Long> badges) {
    }

    public static String markdown(List<PullRequest> pullRequests) {
        List<Standing> standings = standings(pullRequests);
        List<String> rows = IntStream.range(0, standings.size())
                .mapToObj(rank -> row(rank, standings.get(rank)))
                .toList();
        return Stream.of(
                        Stream.of("# 🏆 CUPID workshop leaderboard", "",
                                "Recomputed by the referee after each round. Score = sum of the referee's points over the team's open pull requests, "
                                        + "each one scoring what it adds to the previous step.", ""),
                        standings.isEmpty()
                                ? Stream.of("_No baby step yet._")
                                : Stream.concat(Stream.of("| Rank | Team | Score | Baby steps | Level | Badges |",
                                        "|---|---|---|---|---|---|"), rows.stream()))
                .flatMap(lines -> lines)
                .collect(Collectors.joining("\n", "", "\n"));
    }

    static List<Standing> standings(List<PullRequest> pullRequests) {
        Map<String, List<Play>> byTeam = pullRequests.stream()
                .flatMap(pr -> lastMarker(pr).map(marker -> new Play(team(pr.headRefName()), pr.number(), marker)).stream())
                .collect(Collectors.groupingBy(Play::team));
        return byTeam.entrySet().stream()
                .map(entry -> standing(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(Standing::score).reversed().thenComparing(Standing::team))
                .toList();
    }

    private static Standing standing(String team, List<Play> plays) {
        int score = plays.stream().mapToInt(play -> play.marker().score()).sum();
        long babySteps = plays.stream().filter(play -> play.marker().outcome().equals(Referee.Outcome.ACCEPTED.name())).count();
        String level = plays.stream().max(Comparator.comparingInt(Play::number))
                .map(play -> play.marker().level()).orElse("-");
        Map<String, Long> badges = plays.stream()
                .flatMap(play -> play.marker().badges().stream())
                .collect(Collectors.groupingBy(Function.identity(), java.util.TreeMap::new, Collectors.counting()));
        return new Standing(team, score, babySteps, level, badges);
    }

    private static String row(int rank, Standing standing) {
        String medal = rank < MEDALS.size() ? MEDALS.get(rank) : String.valueOf(rank + 1);
        String badges = standing.badges().entrySet().stream()
                .map(badge -> badge.getKey() + " ×" + badge.getValue())
                .collect(Collectors.joining(", "));
        return "| " + medal + " | " + standing.team() + " | " + standing.score() + " | " + standing.babySteps()
                + " | " + standing.level() + " | " + badges + " |";
    }

    // Seuls les commentaires du bot de l'Action comptent : un étudiant ne peut pas s'attribuer des points.
    private static Optional<Scorecard.Marker> lastMarker(PullRequest pr) {
        return Optional.ofNullable(pr.comments()).orElse(List.of()).stream()
                .filter(comment -> Optional.ofNullable(comment.author()).map(Author::login).orElse("").contains("github-actions"))
                .map(Comment::body)
                .filter(body -> body != null && body.contains(Scorecard.MARKER))
                .reduce((first, second) -> second)
                .map(Leaderboard::marker);
    }

    private static Scorecard.Marker marker(String body) {
        String json = body.substring(body.lastIndexOf(Scorecard.MARKER) + Scorecard.MARKER.length(), body.lastIndexOf(" -->"));
        try {
            return JSON.readValue(json, Scorecard.Marker.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String team(String branch) {
        return branch.contains("/") ? branch.substring(0, branch.indexOf('/')) : branch;
    }

    public static void main(String[] args) {
        if (args.length < 2 || !args[0].equals("--prs")) {
            System.err.println("Usage: leaderboard --prs <prs.json> [--out <leaderboard.md>]");
            System.exit(2);
        }
        try {
            List<PullRequest> pullRequests = JSON.readValue(Path.of(args[1]).toFile(), new TypeReference<List<PullRequest>>() { });
            String markdown = markdown(pullRequests);
            if (args.length >= 4 && args[2].equals("--out")) {
                Files.writeString(Path.of(args[3]), markdown);
            } else {
                System.out.print(markdown);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
