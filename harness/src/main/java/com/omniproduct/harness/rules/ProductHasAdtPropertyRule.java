package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
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

// HA4.3 (moteur AST) - Composable, étape 2 : Product porte ses différences par composition, pas par sous-classes.
// `Product` a au moins une propriété (champ, composant de record ou accesseur sans paramètre) typée par une ADT, c'est-à-dire
// une `sealed interface` dont les alternatives sont des value objects. Cette propriété n'a pas de setter.
// Le nom du champ n'est pas imposé.
public final class ProductHasAdtPropertyRule implements HarnessRule {

    private static final String PRODUCT = "Product";
    private static final Set<String> MUTATORS = Set.of("set", "change", "update", "replace", "assign", "put");

    private final Predicate<String> packageFilter;

    public ProductHasAdtPropertyRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    // Une propriété typée par une ADT : son nom, son type et le nœud où la signaler.
    private record Member(Node at, String name, String type) {
    }

    @Override
    public String id() {
        return "HA4.3";
    }

    @Override
    public String name() {
        return "Product has at least one property typed by an ADT (sealed interface of value objects), no setter";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        List<Node> products = SourceTypes.allDeclarations(sources)
                .filter(type -> SourceTypes.isClass(type) || SourceTypes.isRecord(type))
                .filter(type -> PRODUCT.equals(SourceTypes.name(type)))
                .toList();
        if (products.isEmpty()) {
            return List.of(new Violation(firstPath(sources), 1,
                    "No '" + PRODUCT + "' type found. It must have at least one property typed by an ADT (sealed interface)."));
        }

        Map<String, List<String>> adts = SourceTypes.adts(sources);
        Map<String, Node> valueObjects = SourceTypes.allDeclarations(sources)
                .filter(SourceTypes::isValueObject)
                .collect(Collectors.toMap(SourceTypes::name, type -> type, (first, second) -> first));

        return products.stream()
                .flatMap(product -> check(product, adts, valueObjects))
                .toList();
    }

    private static Stream<Violation> check(Node product, Map<String, List<String>> adts, Map<String, Node> valueObjects) {
        List<Member> members = members(product, adts).toList();
        if (members.isEmpty()) {
            return Stream.of(at(product, "'" + PRODUCT + "' has no property typed by an ADT. Model the alternatives "
                    + "(a sealed interface permitting value-object records) and give Product one property of that type, "
                    + "not a subclass."));
        }
        return members.stream().flatMap(member -> Stream.concat(
                setter(product, member).stream(),
                alternatives(member, adts, valueObjects)));
    }

    private static Stream<Member> members(Node product, Map<String, List<String>> adts) {
        Stream<Member> components = SourceTypes.components(product)
                .filter(component -> isAdt(component, adts))
                .map(component -> new Member(component, component.field("name").map(Node::text).orElse(""),
                        typeName(component)));
        Stream<Member> fields = SourceTypes.members(product)
                .filter(member -> member.is("field_declaration"))
                .filter(field -> isAdt(field, adts))
                .flatMap(field -> field.children().filter(child -> child.is("variable_declarator"))
                        .map(declarator -> new Member(declarator, declarator.field("name").map(Node::text).orElse(""),
                                typeName(field))));
        Stream<Member> getters = SourceTypes.members(product)
                .filter(member -> member.is("method_declaration") && parameterCount(member) == 0)
                .filter(method -> isAdt(method, adts))
                .map(method -> new Member(method, method.field("name").map(Node::text).orElse(""), typeName(method)));
        return Stream.of(components, fields, getters).flatMap(members -> members);
    }

    private static boolean isAdt(Node node, Map<String, List<String>> adts) {
        return node.field("type").map(type -> adts.containsKey(SourceTypes.typeName(type))).orElse(false);
    }

    private static String typeName(Node node) {
        return node.field("type").map(SourceTypes::typeName).orElse("");
    }

    private static long parameterCount(Node method) {
        return method.field("parameters").stream().flatMap(Node::children).count();
    }

    // setShelf, changeShelf, updateStorageShelf... pour la propriété `shelf` : elle ne doit pas être modifiable après
    // construction. Un verbe de mutation suivi des mots de la propriété ; withShelf renvoie une copie, il est permis.
    private static Optional<Violation> setter(Node product, Member member) {
        List<String> memberWords = Words.of(member.name());
        return SourceTypes.members(product)
                .filter(method -> method.is("method_declaration"))
                .filter(method -> method.field("name").map(name -> isMutatorOf(Words.of(name.text()), memberWords)).orElse(false))
                .findFirst()
                .map(method -> at(method, "'" + PRODUCT + "." + member.name() + "' has a setter. Set it once at creation "
                        + "(final field, no setter)."));
    }

    private static boolean isMutatorOf(List<String> methodWords, List<String> memberWords) {
        return !methodWords.isEmpty() && MUTATORS.contains(methodWords.get(0)) && Words.contains(methodWords, memberWords);
    }

    private static Stream<Violation> alternatives(Member member, Map<String, List<String>> adts, Map<String, Node> valueObjects) {
        return adts.getOrDefault(member.type(), List.of()).stream()
                .filter(alternative -> !valueObjects.containsKey(alternative))
                .map(alternative -> at(member.at(), "'" + PRODUCT + "." + member.name() + "': alternative '" + alternative
                        + "' of '" + member.type() + "' is not a value object (a record that does not implement Identifiable)."));
    }

    private static Violation at(Node node, String message) {
        return new Violation(node.file().path(), SourceTypes.nameOf(node).line(), message);
    }

    private static String firstPath(SourceSet sources) {
        return sources.files().stream().findFirst().map(SourceFile::path).orElse("<no files>");
    }
}
