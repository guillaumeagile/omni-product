package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Identifiers;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.SourceTypes;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA17 (moteur AST, erreur) - Tell, don't ask : un service ne reconstruit pas un objet du domaine champ par champ à
// partir d'un autre objet du même type. `new ProductStock(new Quantity(stock.stock().value() - 1), stock.quantity(),
// stock.warehouse())` lit trois accesseurs de `stock` pour fabriquer un `ProductStock` : c'est à `ProductStock` de
// dire `withdrawOne()`. Heuristiques :
//   - un paramètre de méthode ou de constructeur dont le type est déclaré dans les sources analysées (record, classe,
//     interface : pas un type JDK ni de bibliothèque) ;
//   - une construction de CE type dans le même corps : `new T(...)`, ou une fabrique statique `T.xxx(...)` (le nom de
//     la fabrique n'importe pas : of, create, from, with, rebuild...) ;
//   - les arguments de la construction lisent au moins 2 accesseurs DISTINCTS du paramètre (`x()`, `getX()`, `fetchX()`
//     comptent pour le même mot `x`, un champ `p.x` aussi), directement, en chaîne (`stock.stock().value()`), ou par
//     une variable locale initialisée avec cette lecture (`var q = stock.quantity();`) ;
//   - toutes les méthodes du package filtré, privées comprises : déplacer la reconstruction dans une aide privée ne la
//     cache pas.
// Limites : un paramètre sans type déclaré dans les sources, ou un objet lu par la boucle d'une lambda dont le
// paramètre n'est pas un paramètre de méthode, n'est pas suivi.
public final class TellDontAskRule implements HarnessRule {

    private static final int MIN_READS = 2;
    private static final Set<String> ACCESSOR_PREFIXES = Set.of("get", "fetch", "load", "read");

    private final Predicate<String> packageFilter;

    public TellDontAskRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA17";
    }

    @Override
    public String name() {
        return "Tell, don't ask: a service does not rebuild a domain object field by field from another one of the same type";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        Set<String> declared = SourceTypes.allDeclarations(sources).map(SourceTypes::name).collect(Collectors.toUnmodifiableSet());
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(file -> file.descendants()
                        .filter(node -> node.is("method_declaration", "constructor_declaration"))
                        .flatMap(method -> violations(file, method, declared)))
                .toList();
    }

    private static Stream<Violation> violations(SourceFile file, Node method, Set<String> declared) {
        return parameters(method).flatMap(parameter -> {
            String type = parameter.field("type").map(Identifiers::simpleName).orElse("");
            String name = parameter.field("name").map(Node::text).orElse("");
            Map<String, Set<String>> aliases = aliases(method, name);
            return declared.contains(type)
                    ? method.descendants()
                            .filter(node -> isConstructionOf(node, type))
                            .filter(node -> reads(node, name, aliases).size() >= MIN_READS)
                            .map(node -> violation(file, method, node, name, type, reads(node, name, aliases)))
                    : Stream.empty();
        });
    }

    private static Stream<Node> parameters(Node method) {
        return method.field("parameters").stream().flatMap(Node::children).filter(child -> child.is("formal_parameter"));
    }

    private static boolean isConstructionOf(Node node, String type) {
        return node.is("object_creation_expression") && node.field("type").map(Identifiers::simpleName).filter(type::equals).isPresent()
                || node.is("method_invocation") && node.field("object").filter(object -> object.is("identifier"))
                .map(Node::text).filter(type::equals).isPresent();
    }

    // Les accesseurs distincts du paramètre lus par les arguments de la construction.
    private static Set<String> reads(Node construction, String parameter, Map<String, Set<String>> aliases) {
        return construction.field("arguments").stream()
                .flatMap(Node::selfAndDescendants)
                .flatMap(node -> readsOf(node, parameter, aliases))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Stream<String> readsOf(Node node, String parameter, Map<String, Set<String>> aliases) {
        if (node.is("method_invocation") && isParameter(node.field("object"), parameter)) {
            return node.field("name").stream().map(Node::text).map(TellDontAskRule::property);
        }
        if (node.is("field_access") && isParameter(node.field("object"), parameter)) {
            return node.field("field").stream().map(Node::text).map(TellDontAskRule::property);
        }
        return node.is("identifier") ? aliases.getOrDefault(node.text(), Set.of()).stream() : Stream.empty();
    }

    private static boolean isParameter(Optional<Node> object, String parameter) {
        return object.filter(node -> node.is("identifier")).map(Node::text).filter(parameter::equals).isPresent();
    }

    // Variable locale -> accesseurs du paramètre qu'elle porte, directement ou par une autre variable (point fixe).
    private static Map<String, Set<String>> aliases(Node method, String parameter) {
        List<Node> declarators = method.descendants().filter(node -> node.is("variable_declarator")).toList();
        return Stream.iterate(Map.<String, Set<String>>of(), known -> declarators.stream()
                        .flatMap(declarator -> declarator.field("name").map(Node::text).stream().map(alias -> Map.entry(alias, declarator.field("value").stream()
                                .flatMap(Node::selfAndDescendants)
                                .flatMap(node -> readsOf(node, parameter, known))
                                .collect(Collectors.toUnmodifiableSet()))))
                        .filter(entry -> !entry.getValue().isEmpty())
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first)))
                .skip(declarators.size() + 1L)
                .findFirst()
                .orElse(Map.of());
    }

    // getQuantity, fetchQuantity, quantity, Quantity -> « quantity » : seul le dernier mot utile compte.
    private static String property(String accessor) {
        List<String> words = Words.of(accessor);
        return words.stream().skip(words.size() > 1 && ACCESSOR_PREFIXES.contains(words.get(0)) ? 1 : 0)
                .collect(Collectors.joining("_"));
    }

    private static Violation violation(SourceFile file, Node method, Node construction, String parameter, String type, Set<String> reads) {
        String name = method.field("name").map(Node::text).orElse("?");
        return new Violation(file.path(), construction.line(), "'" + name + "' rebuilds a " + type + " from " + reads.size()
                + " accessors of '" + parameter + "'. Tell it instead: give " + type + " the behavior (e.g. "
                + parameter + ".withdrawOne()) so the service stops reading its fields.");
    }
}
