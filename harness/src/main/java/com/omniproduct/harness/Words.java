package com.omniproduct.harness;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

// Les règles raisonnent sur des mots, pas sur des chaînes exactes : `getProductSlug`, `fetch_slug`, `SLUGS` et
// `slug()` parlent tous du slug. Un nom est découpé (camelCase, snake_case, acronymes), mis en minuscules et
// ramené au singulier, ce qui rend la comparaison insensible à la casse, au séparateur et au pluriel.
public final class Words {

    private static final Set<String> IDENTIFIER_WORDS = Set.of("id", "identifier", "uuid", "guid", "uid");

    private Words() {
    }

    // getProductSlug -> [get, product, slug] ; SUPPLIER_IDS -> [supplier, id] ; UUIDv7 -> [uuid, v, 7]
    public static List<String> of(String name) {
        return Arrays.stream(name.split("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|[^A-Za-z0-9]+|(?<=[A-Za-z])(?=[0-9])"))
                .filter(word -> !word.isEmpty())
                .map(String::toLowerCase)
                .map(Words::singular)
                .toList();
    }

    // Suppose que `sub` apparaît dans `words`, de façon contiguë et dans l'ordre : [display, label] dans [get, display, label].
    public static boolean contains(List<String> words, List<String> sub) {
        return !sub.isEmpty() && IntStream.rangeClosed(0, words.size() - sub.size())
                .anyMatch(start -> words.subList(start, start + sub.size()).equals(sub));
    }

    // Un nom parle d'identité dès qu'un de ses mots est id / identifier / uuid / guid / uid : productId, ID, product_id,
    // idProduct, supplierIds, uuid. Les mots entiers évitent les faux positifs (valid, android, paid).
    public static boolean isIdentifier(String name) {
        return of(name).stream().anyMatch(IDENTIFIER_WORDS::contains);
    }

    private static String singular(String word) {
        if (word.length() > 3 && word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        }
        return word.length() > 2 && word.endsWith("s") && !word.endsWith("ss") ? word.substring(0, word.length() - 1) : word;
    }
}
