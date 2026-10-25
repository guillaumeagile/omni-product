package com.omniproduct.harness;

import java.util.List;
import java.util.regex.Pattern;

// Compte les tokens d'un texte : la mesure de coût d'un agent qui lit ou écrit ce code. Proxy déterministe, pas le
// tokenizer d'un modèle précis : les identifiants se découpent aux majuscules et aux underscores (getSellByDate -> get,
// Sell, By, Date), les nombres et chaque caractère de ponctuation ou opérateur comptent pour un token.
public final class Tokens {

    private static final Pattern TOKEN = Pattern.compile("[A-Z]?[a-z]+|[A-Z]+(?![a-z])|\\d+|\\S");

    private Tokens() {
    }

    public static long count(String text) {
        return TOKEN.matcher(text).results().count();
    }

    // Tokens du diff unifié (git diff -U0) : seules les lignes ajoutées ou retirées comptent, sans les en-têtes.
    public static long countChanged(List<String> diffLines) {
        return diffLines.stream()
                .filter(line -> (line.startsWith("+") && !line.startsWith("+++")) || (line.startsWith("-") && !line.startsWith("---")))
                .mapToLong(line -> count(line.substring(1)))
                .sum();
    }

    // Usage (dans l'Action) : tokens --diff diff.txt, affiche le nombre de tokens modifiés.
    public static void main(String[] args) throws java.io.IOException {
        if (args.length != 2 || !args[0].equals("--diff")) {
            System.err.println("Usage: tokens --diff <fichier-diff>");
            System.exit(2);
        }
        System.out.println(countChanged(java.nio.file.Files.readAllLines(java.nio.file.Path.of(args[1]))));
    }
}
