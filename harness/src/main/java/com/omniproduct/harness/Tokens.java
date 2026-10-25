package com.omniproduct.harness;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

// Compte les tokens d'un texte : la mesure de coût d'un agent qui lit ou écrit ce code. Compté par JTokkit (le BPE de
// tiktoken, encodage o200k_base), outil éprouvé et déterministe. Ce n'est pas le tokenizer de tous les modèles (celui
// de Claude n'est pas public), mais l'ordre de grandeur et les variations d'une PR à l'autre sont fidèles.
public final class Tokens {

    private static final Encoding ENCODING = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.O200K_BASE);

    private Tokens() {
    }

    public static long count(String text) {
        return ENCODING.countTokensOrdinary(text);
    }

    // Tokens du diff unifié (git diff -U0) : seules les lignes ajoutées ou retirées comptent, sans les en-têtes.
    public static long countChanged(List<String> diffLines) {
        return count(diffLines.stream()
                .filter(line -> (line.startsWith("+") && !line.startsWith("+++")) || (line.startsWith("-") && !line.startsWith("---")))
                .map(line -> line.substring(1))
                .collect(Collectors.joining("\n")));
    }

    // Usage (dans l'Action) : tokens --diff diff.txt, affiche le nombre de tokens modifiés.
    public static void main(String[] args) throws IOException {
        if (args.length != 2 || !args[0].equals("--diff")) {
            System.err.println("Usage: tokens --diff <fichier-diff>");
            System.exit(2);
        }
        System.out.println(countChanged(Files.readAllLines(Path.of(args[1]))));
    }
}
