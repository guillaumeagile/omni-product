package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Identifiers;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA11 (moteur AST) - les identifiants sont des objets riches : tout `id` / `*Id` / `*Ids` (champ, variable
// locale, retour, paramètre, composant de record) est un UUID ou un Ulid, ou un record qui en enveloppe
// exactement un (record ProductId(UUID value)). Jamais String / int / long. Pour une collection ou une map
// d'identifiants, ce sont les types d'éléments et de clés qui comptent.
// « Au moins v7 » se vérifie par la forme : UUID.randomUUID() (v4) et new UUID(...) sont signalés là où ils
// construisent un identifiant ; UuidCreator.getTimeOrderedEpoch() et UlidCreator.getUlid() passent.
public final class RichIdentifierRule implements HarnessRule {

    private static final Set<String> RICH = Set.of("UUID", "Ulid");

    private final Predicate<String> packageFilter;

    public RichIdentifierRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA11";
    }

    @Override
    public String name() {
        return "Identifiers are rich objects: UUID/Ulid (or a record wrapping one), v7 or later";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        Map<String, Node> declaredTypes = declaredTypes(sources);
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(file -> Stream.of(poorTypes(file, declaredTypes), randomIdentifiers(file)).flatMap(violations -> violations))
                .distinct()
                .toList();
    }

    private static Stream<Violation> poorTypes(SourceFile file, Map<String, Node> declaredTypes) {
        return Identifiers.uses(file).flatMap(use -> Identifiers.leafTypes(use.type())
                .map(Identifiers::simpleName)
                .flatMap(type -> problem(type, declaredTypes)
                        .map(problem -> new Violation(file.path(), use.name().line(),
                                "Identifier '" + use.name().text() + "' " + problem)).stream()));
    }

    private static Optional<String> problem(String type, Map<String, Node> declaredTypes) {
        if (RICH.contains(type)) {
            return Optional.empty();
        }
        // Tout ce qui n'est ni UUID/Ulid ni un type du projet est un identifiant pauvre (String, long, BigInteger,
        // CharSequence, Object, ...) : on ne tient pas de liste de types interdits, qu'il suffirait de contourner.
        return Optional.ofNullable(declaredTypes.get(type))
                .map(declaration -> Optional.of(declaration).filter(d -> !wrapsExactlyOneRichValue(d))
                        .map(d -> "has type '" + type + "' which is not a record wrapping exactly one UUID or Ulid."))
                .orElseGet(() -> Optional.of("is a " + type + ": use a UUID (v7) or Ulid, ideally wrapped in a record."));
    }

    private static boolean wrapsExactlyOneRichValue(Node declaration) {
        List<String> componentTypes = declaration.field("parameters").stream()
                .flatMap(Node::children)
                .map(parameter -> parameter.field("type").map(Identifiers::simpleName).orElse(""))
                .toList();
        return declaration.is("record_declaration") && componentTypes.size() == 1 && RICH.contains(componentTypes.get(0));
    }

    // Tout UUID qui n'est pas un v7, où qu'il soit construit dans le modèle (initialiseur, helper `newId()`, lambda,
    // référence de méthode) : on ne suit pas le flux de données jusqu'au champ, un modèle n'a aucune raison d'en fabriquer un.
    private static Stream<Violation> randomIdentifiers(SourceFile file) {
        return file.descendants()
                .filter(RichIdentifierRule::buildsNonV7Uuid)
                .map(node -> new Violation(file.path(), node.line(),
                        "'" + node.text() + "' builds a UUID v4 / empty UUID: use a v7 (UuidCreator.getTimeOrderedEpoch()) or a Ulid."));
    }

    private static boolean buildsNonV7Uuid(Node node) {
        return switch (node.type()) {
            case "method_invocation" -> {
                String method = node.field("name").map(Node::text).orElse("");
                // Sans objet, c'est un import statique : `randomUUID()`.
                String owner = node.field("object").map(RichIdentifierRule::simpleName).orElse("UUID");
                yield (owner.equals("UUID") && Set.of("randomUUID", "nameUUIDFromBytes").contains(method))
                        || (owner.equals("UuidCreator") && method.startsWith("get") && !method.startsWith("getTimeOrderedEpoch"));
            }
            case "object_creation_expression" -> node.field("type").map(RichIdentifierRule::simpleName).filter("UUID"::equals).isPresent();
            case "method_reference" -> node.text().matches(".*\\bUUID::randomUUID");
            default -> false;
        };
    }

    // "java.util.UUID" et "UUID" désignent le même type.
    private static String simpleName(Node node) {
        return Identifiers.simpleName(node);
    }

    private static Map<String, Node> declaredTypes(SourceSet sources) {
        return sources.files().stream()
                .flatMap(SourceFile::descendants)
                .filter(node -> node.is("class_declaration", "record_declaration", "interface_declaration", "enum_declaration"))
                .flatMap(node -> node.field("name").map(name -> Map.entry(name.text(), node)).stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first));
    }
}
