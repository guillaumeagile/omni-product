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

// HA4.9 (moteur AST) - Composable : la fabrique d'un value object valide par composition, pas par branchement.
// Un `if (a == null || b.isBefore(a)) return none();` répond à la question « est-ce valide ? » en coupant le flot ;
// `Option.of(a).flatMap(s -> Option.of(b).filter(u -> !u.isBefore(s)).map(u -> new Perishable(s, u)))` ou
// `Validation.combine(...).ap(Perishable::new)` la pose étape par étape, chacune nommée, chacune une valeur.
// Fabrique = méthode statique non privée d'un value object dont le nom porte un mot de fabrique (create, build, of,
// from, make, comme HA4.4) OU dont le type de retour nomme le record (`Option<Perishable>`, `PerishableCreation`) :
// renommer `of` en `parse` n'échappe pas. Sont lus le corps de la fabrique et, transitivement, les méthodes du même
// type qu'elle appelle (extraire le `if` dans un `validate` privé n'échappe pas non plus ; le helper privé n'est pas
// une fabrique à son tour, le défaut est signalé une fois).
// Signalés : un `if`, un ternaire, un `switch` ; un `throw`, un `assert`, un appel de garde (requireNonNull,
// checkArgument...) ; et, à défaut de branchement, l'absence totale de lambda ou de référence de méthode alors que
// le type de retour promet un échec (type générique ou ADT scellée du code) : la fabrique ne décide rien.
// Existence : la règle est rouge s'il n'y a aucune fabrique dont le résultat peut échouer (type générique ou ADT
// scellée). Une règle verte parce qu'il n'y a rien à examiner ne prouverait rien ; une fabrique triviale qui renvoie
// le record nu (`Slug.of(raw)`) ne compte pas non plus.
public final class FactoryValidatesByCompositionRule implements HarnessRule {

    private static final Set<String> FACTORY_WORDS = Set.of("create", "build", "of", "from", "make");
    private static final Set<String> GUARD_WORDS = Set.of("throw", "require", "check", "assert");
    private static final Set<String> BRANCHES = Set.of("if_statement", "ternary_expression", "switch_expression", "switch_statement");
    private static final Set<String> THROWS = Set.of("throw_statement", "assert_statement");
    private static final Set<String> COMPOSITION = Set.of("lambda_expression", "method_reference");

    private final Predicate<String> packageFilter;

    public FactoryValidatesByCompositionRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA4.9";
    }

    @Override
    public String name() {
        return "A value object factory validates by composing Option/Validation steps, not with if/throw";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        Set<String> adts = SourceTypes.adts(sources).keySet();
        List<SourceFile> model = sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .toList();
        List<Violation> smells = model.stream()
                .flatMap(file -> SourceTypes.declarations(file)
                        .filter(SourceTypes::isValueObject)
                        .flatMap(record -> factories(record)
                                .flatMap(factory -> violations(file, record, factory, adts))))
                .toList();
        boolean existsAndCanFail = model.stream().anyMatch(file -> SourceTypes.declarations(file)
                .filter(SourceTypes::isValueObject)
                .anyMatch(record -> factories(record).anyMatch(factory -> promisesFailure(factory, adts))));
        return Stream.concat(smells.stream(), absence(model, existsAndCanFail)).toList();
    }

    // Rien à composer, rien à juger : une règle qui passe par vide ne prouve rien (comme HA4.4, elle est rouge tant
    // qu'aucun value object n'a une fabrique dont le résultat peut échouer).
    private static Stream<Violation> absence(List<SourceFile> model, boolean existsAndCanFail) {
        if (existsAndCanFail) {
            return Stream.empty();
        }
        return Stream.of(new Violation(
                model.stream().findFirst().map(SourceFile::path).orElse("<no files>"), 1,
                "No value object has a static factory whose result can fail (a generic result such as Option/Either/"
                        + "Validation, or a sealed result). Write one that validates by composing steps "
                        + "(filter, flatMap, map, combine), not with if/throw."));
    }

    private static Stream<Node> factories(Node record) {
        String self = SourceTypes.name(record);
        return SourceTypes.members(record)
                .filter(member -> member.is("method_declaration") && member.hasModifier("static"))
                .filter(method -> !method.hasModifier("private"))
                .filter(method -> hasFactoryName(method) || returns(method, self));
    }

    private static boolean hasFactoryName(Node method) {
        return method.field("name")
                .map(name -> Words.of(name.text()).stream().anyMatch(FACTORY_WORDS::contains))
                .orElse(false);
    }

    // Option<Perishable>, Either<InvalidDates, Perishable>, PerishableCreation, Perishable : le retour parle du record.
    private static boolean returns(Node method, String self) {
        return method.field("type")
                .map(type -> SourceTypes.typeName(type).equals(self)
                        || SourceTypes.typeArguments(type).stream().map(SourceTypes::typeName).anyMatch(self::equals)
                        || type.descendants().filter(node -> node.is("type_identifier")).map(Node::text).anyMatch(self::equals))
                .orElse(false);
    }

    private static Stream<Violation> violations(SourceFile file, Node record, Node factory, Set<String> adts) {
        String where = "Factory '" + SourceTypes.name(factory) + "' of '" + SourceTypes.name(record) + "'";
        List<Node> body = reachableNodes(record, factory);
        List<Violation> branching = body.stream()
                .flatMap(node -> smell(node).stream()
                        .map(what -> new Violation(file.path(), node.line(), where + " " + what
                                + ". Validate by composing Option/Validation/Either steps (filter, flatMap, map, combine) "
                                + "that return the result, instead of branching or throwing.")))
                .distinct()
                .toList();
        if (!branching.isEmpty()) {
            return branching.stream();
        }
        boolean composes = body.stream().anyMatch(node -> node.is(COMPOSITION.toArray(String[]::new)));
        if (composes || !promisesFailure(factory, adts)) {
            return Stream.empty();
        }
        return Stream.of(new Violation(file.path(), factory.line(), where
                + " returns a result that can fail but never decides anything: no lambda nor method reference. "
                + "Validate the input by composing Option/Validation/Either steps (filter, flatMap, map, combine)."));
    }

    private static Optional<String> smell(Node node) {
        if (node.is(BRANCHES.toArray(String[]::new))) {
            return Optional.of("branches with '" + keyword(node) + "'");
        }
        if (node.is(THROWS.toArray(String[]::new))) {
            return Optional.of("contains '" + keyword(node) + "'");
        }
        return node.field("name")
                .filter(name -> node.is("method_invocation"))
                .filter(name -> Words.of(name.text()).stream().anyMatch(GUARD_WORDS::contains))
                .map(name -> "calls the guard '" + name.text() + "'");
    }

    private static String keyword(Node node) {
        return switch (node.type()) {
            case "ternary_expression" -> "?:";
            case "if_statement" -> "if";
            case "switch_expression", "switch_statement" -> "switch";
            case "throw_statement" -> "throw";
            case "assert_statement" -> "assert";
            default -> node.type();
        };
    }

    // Un type générique (Option<Perishable>, Validated<Price>) ou une ADT scellée du code : le retour promet un échec.
    private static boolean promisesFailure(Node factory, Set<String> adts) {
        return factory.field("type")
                .map(type -> type.is("generic_type") || adts.contains(SourceTypes.typeName(type)))
                .orElse(false);
    }

    // Les noeuds du corps de la fabrique, plus ceux des méthodes du même type qu'elle appelle (transitivement, sans
    // repasser deux fois par la même méthode).
    private static List<Node> reachableNodes(Node record, Node factory) {
        Map<String, Node> methods = SourceTypes.members(record)
                .filter(member -> member.is("method_declaration"))
                .collect(Collectors.toMap(SourceTypes::name, method -> method, (first, second) -> first));
        return reach(methods, factory, Set.of(SourceTypes.name(factory))).toList();
    }

    private static Stream<Node> reach(Map<String, Node> methods, Node method, Set<String> visited) {
        List<Node> body = method.field("body").stream().flatMap(Node::descendants).toList();
        Set<String> callees = body.stream()
                .filter(node -> node.is("method_invocation") && isLocalCall(node))
                .flatMap(node -> node.field("name").stream().map(Node::text))
                .filter(methods::containsKey)
                .filter(name -> !visited.contains(name))
                .collect(Collectors.toSet());
        Set<String> seen = Stream.concat(visited.stream(), callees.stream()).collect(Collectors.toSet());
        return Stream.concat(body.stream(), callees.stream().flatMap(name -> reach(methods, methods.get(name), seen)));
    }

    // validate(a, b), this.validate(a, b), Perishable.validate(a, b) : un appel dont le receveur est le type lui-même.
    private static boolean isLocalCall(Node invocation) {
        return invocation.field("object")
                .map(object -> object.is("this") || object.is("identifier") && Character.isUpperCase(object.text().charAt(0)))
                .orElse(true);
    }
}
