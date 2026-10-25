package com.omniproduct.harness;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Le vocabulaire de chaque contexte borné (ubiquitous-language-map.json). Un nom de membre appartient à un contexte
// dès qu'un de ses mots (ou une suite de mots, pour "DisplayLabel") est un mot-clé du contexte, où qu'il soit dans le
// nom : getSlug, fetchSlug, slug(), displaySlug, getProductSlug.
// naturalIdentifiers : les identifiants dont le code est l'identité, attribué hors du système (siren, tva...). HA12 ne
// leur demande pas d'UUID ; tout autre identifiant est généré par le système et doit être riche.
public record UbiquitousLanguageMap(Map<String, List<String>> concerns,
                                    Map<String, List<String>> restrictions,
                                    List<String> weakConcerns,
                                    List<String> naturalIdentifiers) {

    private record Match(String concern, int words, int length) {
    }

    private static final Comparator<Match> MOST_SPECIFIC =
            Comparator.comparingInt(Match::words).thenComparingInt(Match::length);

    public UbiquitousLanguageMap {
        restrictions = restrictions == null ? Map.of() : restrictions;
        weakConcerns = weakConcerns == null ? List.of() : weakConcerns;
        naturalIdentifiers = naturalIdentifiers == null ? List.of() : naturalIdentifiers;
    }

    public static UbiquitousLanguageMap load() {
        try (InputStream in = UbiquitousLanguageMap.class.getResourceAsStream("/ubiquitous-language-map.json")) {
            return new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(in, UbiquitousLanguageMap.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Le mot-clé le plus spécifique l'emporte ; un contexte « faible » (Lifecycle : tout s'appelle Product) ne compte
    // que si rien d'autre ne correspond. Aucun contexte si le nom est générique (getId, findById).
    public Optional<String> concernOf(String memberName) {
        List<String> words = Words.of(memberName);
        List<Match> matches = concerns.entrySet().stream()
                .flatMap(concern -> concern.getValue().stream()
                        .filter(keyword -> Words.contains(words, Words.of(keyword)))
                        .map(keyword -> new Match(concern.getKey(), Words.of(keyword).size(), keyword.length())))
                .toList();
        return matches.stream().filter(match -> !weakConcerns.contains(match.concern())).max(MOST_SPECIFIC)
                .or(() -> matches.stream().max(MOST_SPECIFIC))
                .map(Match::concern);
    }

    public Optional<String> forbiddenKeyword(String typeName, String memberName) {
        List<String> words = Words.of(memberName);
        return restrictions.getOrDefault(typeName, List.of()).stream()
                .filter(keyword -> Words.contains(words, Words.of(keyword)))
                .findFirst();
    }
}
