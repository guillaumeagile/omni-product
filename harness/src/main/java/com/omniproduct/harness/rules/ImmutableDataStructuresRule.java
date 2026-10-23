package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA2 (moteur AST) - structures de données immuables : des records, aucun setter public, aucune
// collection mutable de java.util. Les collections se résolvent d'après les imports du fichier :
// `List` importé de java.util est mutable, `List` importé de io.vavr.collection ne l'est pas.
public final class ImmutableDataStructuresRule implements HarnessRule {

    private static final Pattern SETTER = Pattern.compile("^set[A-Z]\\w*$");
    private static final Set<String> MUTABLE_COLLECTIONS = Set.of(
            "Collection", "List", "Set", "Map", "Queue", "Deque", "SortedSet", "SortedMap",
            "ArrayList", "LinkedList", "Vector", "Stack", "ArrayDeque", "PriorityQueue",
            "HashSet", "LinkedHashSet", "TreeSet", "HashMap", "LinkedHashMap", "TreeMap");

    private final Predicate<String> packageFilter;

    public ImmutableDataStructuresRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA2";
    }

    @Override
    public String name() {
        return "Immutable data structures (records only, no setters, no mutable collections)";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(ImmutableDataStructuresRule::violations)
                .distinct()
                .toList();
    }

    private static Stream<Violation> violations(SourceFile file) {
        Set<String> imports = imports(file);
        return file.descendants().flatMap(node -> switch (node.type()) {
            case "class_declaration" -> node.field("name").stream()
                    .map(name -> violation(file, name, "'" + name.text() + "' is a class: model data structures must be records."));
            case "method_declaration" -> setter(node)
                    .map(name -> violation(file, name, "'" + name.text() + "' is a setter: records have no setters, return a new value instead."))
                    .stream();
            case "type_identifier", "scoped_type_identifier" -> mutableCollection(node, imports)
                    .map(collection -> violation(file, node, "'" + collection + "' is a mutable java.util collection: use an immutable one (vavr, List.copyOf, ...)."))
                    .stream();
            default -> Stream.empty();
        });
    }

    private static java.util.Optional<Node> setter(Node method) {
        return method.field("name")
                .filter(name -> method.hasModifier("public") && SETTER.matcher(name.text()).matches())
                .filter(name -> method.field("parameters").stream().flatMap(Node::children).count() == 1);
    }

    private static java.util.Optional<String> mutableCollection(Node type, Set<String> imports) {
        String text = type.text();
        String simple = text.substring(text.lastIndexOf('.') + 1);
        boolean fromJavaUtil = type.is("scoped_type_identifier")
                ? text.equals("java.util." + simple)
                : imports.contains("java.util." + simple) || imports.contains("java.util.*");
        return java.util.Optional.of(simple).filter(MUTABLE_COLLECTIONS::contains).filter(name -> fromJavaUtil);
    }

    // "import java.util.List;" -> "java.util.List" ; "import java.util.*;" -> "java.util.*"
    private static Set<String> imports(SourceFile file) {
        return file.root().children()
                .filter(node -> node.is("import_declaration"))
                .map(node -> node.text().replaceFirst("^import\\s+(static\\s+)?", "").replaceFirst("\\s*;\\s*$", ""))
                .collect(Collectors.toSet());
    }

    private static Violation violation(SourceFile file, Node at, String message) {
        return new Violation(file.path(), at.line(), message);
    }
}
