package com.omniproduct.harness.referee;

import com.omniproduct.harness.HarnessReport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Ligne de commande de l'arbitre, appelée par la GitHub Action :
//   referee --base base.json --head head.json --title "HA9 green: ..." --branch step-1/ha9 \
//           [--changed-files fichiers.txt] [--changed-lines 42] [--small-diff 50] [--out commentaire.md]
// Un rapport absent (le projet ne compile pas) est un rapport vide, pas une erreur d'usage.
// Code de sortie : 0 accepté ou hors jeu, 1 refusé, 2 mauvais usage.
public final class RefereeMain {

    private static final List<String> KEYS =
            List.of("--base", "--head", "--title", "--branch", "--changed-files", "--changed-lines", "--small-diff", "--out");

    private RefereeMain() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    public static int run(String[] args) {
        if (args.length % 2 != 0 || IntStream.range(0, args.length / 2).anyMatch(i -> !KEYS.contains(args[2 * i]))) {
            System.err.println("Usage: referee --base <json> --head <json> --title <titre> --branch <branche> "
                    + "[--changed-files <fichier>] [--changed-lines <n>] [--small-diff <n>] [--out <md>]");
            return 2;
        }
        Map<String, String> options = IntStream.range(0, args.length / 2).boxed()
                .collect(Collectors.toMap(i -> args[2 * i], i -> args[2 * i + 1], (first, second) -> second));

        Referee.PullRequest pr = new Referee.PullRequest(
                options.getOrDefault("--title", ""),
                options.getOrDefault("--branch", ""),
                Optional.ofNullable(options.get("--changed-files")).map(Path::of).map(RefereeMain::lines).orElse(List.of()),
                Integer.parseInt(options.getOrDefault("--changed-lines", String.valueOf(Integer.MAX_VALUE))),
                Integer.parseInt(options.getOrDefault("--small-diff", "50")));

        Referee.Verdict verdict = Referee.judge(report(options.get("--base")), report(options.get("--head")), pr);
        String markdown = Scorecard.markdown(verdict);

        Optional.ofNullable(options.get("--out")).map(Path::of).ifPresentOrElse(
                out -> write(out, markdown),
                () -> System.out.print(markdown));
        return verdict.outcome() == Referee.Outcome.REJECTED ? 1 : 0;
    }

    private static Optional<HarnessReport> report(String path) {
        return Optional.ofNullable(path).map(Path::of).filter(Files::isRegularFile).map(HarnessReport::read);
    }

    private static List<String> lines(Path file) {
        try {
            return Files.readAllLines(file).stream().map(String::strip).filter(line -> !line.isEmpty()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path file, String content) {
        try {
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
