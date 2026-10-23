package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA5 (moteur AST) - pas de god class : au plus N propriétés publiques par type.
// Une propriété publique est un composant de record, un champ public, une méthode publique sans paramètre qui
// renvoie une valeur (getX, isX, fetchX, x()) ou un setter public (setX). Lire et écrire la même propriété ne
// compte qu'une fois. Les identifiants (id, productId, uuid...) sont ignorés : pas de surface comportementale.
public final class NoGodClassRule implements HarnessRule {

    private final int maxPublicProperties;
    private final Predicate<String> packageFilter;

    public NoGodClassRule(int maxPublicProperties, Predicate<String> packageFilter) {
        this.maxPublicProperties = maxPublicProperties;
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA5";
    }

    @Override
    public String name() {
        return "No god class (at most " + maxPublicProperties + " public properties)";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(SourceFile::descendants)
                .filter(node -> node.is("class_declaration", "record_declaration", "interface_declaration"))
                .flatMap(this::violation)
                .toList();
    }

    private Stream<Violation> violation(Node type) {
        Set<String> properties = publicProperties(type);
        Node name = type.field("name").orElseThrow();
        return Optional.of(properties)
                .filter(props -> props.size() > maxPublicProperties)
                .map(props -> new Violation(type.file().path(), name.line(),
                        "'" + name.text() + "' has " + props.size() + " public properties " + props
                                + " (max " + maxPublicProperties + "). Split into smaller, focused types."))
                .stream();
    }

    // Les verbes qui ne font que lire : getSlug, fetchSlug, loadSlug et slug() exposent tous la propriété « slug ».
    private static final Set<String> READ_VERBS =
            Set.of("get", "is", "has", "fetch", "load", "read", "find", "retrieve", "obtain", "compute", "calculate");
    // Des méthodes sans paramètre qui ne sont pas des propriétés.
    private static final Set<String> NOT_PROPERTIES = Set.of("toString", "hashCode", "clone", "build", "copy", "builder");

    private static Set<String> publicProperties(Node type) {
        // Dans une interface, un membre sans modifieur est public ; seul `private` le cache.
        boolean interfaceType = type.is("interface_declaration");
        List<Node> publicMembers = type.field("body").stream()
                .flatMap(Node::children)
                .filter(member -> (member.hasModifier("public") || (interfaceType && !member.hasModifier("private")))
                        && !member.hasModifier("static"))
                .toList();

        Stream<String> components = type.field("parameters").stream()
                .flatMap(Node::children)
                .flatMap(parameter -> parameter.field("name").stream())
                .map(Node::text);

        Stream<String> fields = publicMembers.stream()
                .filter(member -> member.is("field_declaration"))
                .flatMap(Node::children)
                .filter(declarator -> declarator.is("variable_declarator"))
                .flatMap(declarator -> declarator.field("name").stream())
                .map(Node::text);

        Stream<String> readers = publicMembers.stream()
                .filter(member -> member.is("method_declaration") && parameterCount(member) == 0 && returnsAValue(member))
                .flatMap(method -> method.field("name").stream())
                .map(Node::text)
                .filter(name -> !NOT_PROPERTIES.contains(name) && !name.matches("to[A-Z].*"));

        Stream<String> writers = publicMembers.stream()
                .filter(member -> member.is("method_declaration") && parameterCount(member) == 1)
                .flatMap(method -> method.field("name").stream())
                .map(Node::text)
                .filter(name -> name.matches("set[A-Z].*"));

        return Stream.of(components, fields, readers, writers)
                .flatMap(properties -> properties)
                .filter(name -> !Words.isIdentifier(name))
                .map(NoGodClassRule::property)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static long parameterCount(Node method) {
        return method.field("parameters").stream().flatMap(Node::children).count();
    }

    private static boolean returnsAValue(Node method) {
        return method.field("type").filter(type -> !type.text().equals("void")).isPresent();
    }

    // getName / setName / name -> name : lecture et écriture d'une même propriété ne comptent qu'une fois.
    private static String property(String member) {
        List<String> words = Words.of(member);
        return String.join("", words.size() > 1 && (READ_VERBS.contains(words.get(0)) || words.get(0).equals("set"))
                ? words.subList(1, words.size()) : words);
    }
}
