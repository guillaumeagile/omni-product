package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA5 (moteur AST) - pas de god class : au plus N propriétés publiques par type.
// Une propriété publique est un composant de record, un champ public, ou un getter public
// sans paramètre (getX / isX). Les propriétés dont le nom finit par "Id" sont ignorées :
// ce sont des identifiants, pas de la surface comportementale.
public final class NoGodClassRule implements HarnessRule {

    private static final Pattern ACCESSOR = Pattern.compile("^(?:get|is)([A-Z]\\w*)$");

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
                .filter(node -> node.is("class_declaration", "record_declaration"))
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

    private static Set<String> publicProperties(Node type) {
        List<Node> publicMembers = type.field("body").stream()
                .flatMap(Node::children)
                .filter(member -> member.hasModifier("public"))
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

        Stream<String> accessors = publicMembers.stream()
                .filter(member -> member.is("method_declaration") && hasNoParameters(member))
                .flatMap(method -> method.field("name").stream())
                .map(name -> accessorProperty(name.text()))
                .flatMap(Optional::stream);

        return Stream.of(components, fields, accessors)
                .flatMap(properties -> properties)
                .filter(property -> !property.endsWith("Id"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean hasNoParameters(Node method) {
        return method.field("parameters").stream().allMatch(parameters -> parameters.children().findAny().isEmpty());
    }

    // getName -> name, isActive -> active ; tout autre nom n'est pas un accesseur.
    private static Optional<String> accessorProperty(String method) {
        return Optional.of(ACCESSOR.matcher(method))
                .filter(java.util.regex.Matcher::matches)
                .map(matcher -> matcher.group(1))
                .map(rest -> Character.toLowerCase(rest.charAt(0)) + rest.substring(1));
    }
}
