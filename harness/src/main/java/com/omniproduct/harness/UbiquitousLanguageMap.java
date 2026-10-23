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
import java.util.regex.Pattern;

// Le vocabulaire de chaque contexte borné (ubiquitous-language-map.json).
public record UbiquitousLanguageMap(Map<String, List<String>> concerns, Map<String, List<String>> restrictions) {

    private static final Pattern LEADING_VERB = Pattern.compile(
            "^(?:get|set|is|has|find|create|update|delete|save|calculate|compute|add|remove|with)(?:All)?(?=[A-Z]|$)");

    public UbiquitousLanguageMap {
        restrictions = restrictions == null ? Map.of() : restrictions;
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

    // getSlug -> "Slug" ; findById -> "ById" ; getAllSuppliers -> "Suppliers"
    public static String vocabulary(String memberName) {
        return LEADING_VERB.matcher(memberName).replaceFirst("");
    }

    // Le concern dont le mot-clé le plus long préfixe le nom du membre ; aucun si le nom est générique.
    public Optional<String> concernOf(String memberName) {
        String vocabulary = vocabulary(memberName);
        return concerns.entrySet().stream()
                .flatMap(concern -> concern.getValue().stream()
                        .filter(keyword -> startsWithIgnoreCase(vocabulary, keyword))
                        .map(keyword -> Map.entry(concern.getKey(), keyword)))
                .max(Comparator.comparingInt(match -> match.getValue().length()))
                .map(Map.Entry::getKey);
    }

    public Optional<String> forbiddenKeyword(String typeName, String memberName) {
        return restrictions.getOrDefault(typeName, List.of()).stream()
                .filter(keyword -> startsWithIgnoreCase(vocabulary(memberName), keyword))
                .findFirst();
    }

    private static boolean startsWithIgnoreCase(String text, String prefix) {
        return text.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
