package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.SourceTypes;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA4.8 (moteur AST, erreur) - une collection se transforme par un pipeline (Stream), pas en remplissant un
// accumulateur. Est fautive toute itération qui modifie une variable déclarée hors d'elle :
//   - une boucle `for`, `for (x : xs)`, `while`, `do ... while` ;
//   - un `forEach` / `forEachOrdered` / `peek` dont la lambda ou la référence de méthode modifie l'extérieur
//     (`products.forEach(result::add)`, `IntStream.range(...).forEach(i -> list.add(...))`).
// Modifier = affecter (`=`, `+=`, `++`, `--`) une variable ou un champ, ou appeler sur lui une méthode dont un mot est
// un verbe de remplissage (add, put, append, push, offer, insert, set, remove, merge, compute, increment, accumulate,
// accept, clear). L'extérieur = tout nom qui n'est pas déclaré dans l'itération (variable locale, variable de boucle,
// paramètre de lambda) : peu importe son type, `List`, `Map`, `StringBuilder`, compteur ou champ.
// Une boucle qui ne fait que lire, déléguer ou travailler sur ses propres variables n'est pas visée.
public final class CollectionsArePipelinesRule implements HarnessRule {

    private static final Set<String> LOOPS = Set.of("for_statement", "enhanced_for_statement", "while_statement", "do_statement");
    private static final Set<String> FILL_WORDS = words("add", "put", "append", "push", "offer", "insert", "set", "remove",
            "merge", "compute", "increment", "accumulate", "accept", "clear");

    private final Predicate<String> packageFilter;

    public CollectionsArePipelinesRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    // Les mots de référence passent par le même découpage que les noms (singulier, minuscules).
    private static Set<String> words(String... references) {
        return Stream.of(references).flatMap(reference -> Words.of(reference).stream()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String id() {
        return "HA4.8";
    }

    @Override
    public String name() {
        return "Collections are transformed by Stream pipelines, not by a loop that fills an accumulator";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(CollectionsArePipelinesRule::violations)
                .toList();
    }

    private static Stream<Violation> violations(SourceFile file) {
        return file.descendants()
                .filter(node -> node.is("method_declaration", "constructor_declaration"))
                .flatMap(method -> method.field("body").stream()
                        .flatMap(CollectionsArePipelinesRule::offendingIterations)
                        .map(iteration -> violation(file, method, iteration)));
    }

    // Une itération fautive est signalée une fois ; on ne descend pas dans ses boucles imbriquées.
    private static Stream<Iteration> offendingIterations(Node node) {
        return iteration(node)
                .flatMap(candidate -> candidate.filledVariable().map(variable -> new Iteration(node, variable)))
                .map(Stream::of)
                .orElseGet(() -> node.children().flatMap(CollectionsArePipelinesRule::offendingIterations));
    }

    private static Violation violation(SourceFile file, Node method, Iteration iteration) {
        return new Violation(file.path(), iteration.node().line(),
                "'" + SourceTypes.name(method) + "' fills '" + iteration.variable() + "' while iterating. Build the result"
                        + " with a Stream pipeline instead: stream().filter(...).map(...).toList(),"
                        + " Collectors.groupingBy/partitioningBy/joining, or reduce.");
    }

    // Le corps d'une itération et les noms qu'elle déclare elle-même.
    private static Optional<Candidate> iteration(Node node) {
        if (node.is(LOOPS.toArray(String[]::new))) {
            return Optional.of(new Candidate(node, declaredIn(node)));
        }
        return Optional.of(node)
                .filter(call -> call.is("method_invocation") && isForEach(call))
                .flatMap(call -> call.field("arguments"))
                .flatMap(arguments -> arguments.children().filter(argument -> argument.is("lambda_expression", "method_reference")).findFirst())
                .map(callback -> new Candidate(callback, declaredIn(callback)));
    }

    private static boolean isForEach(Node call) {
        List<String> words = call.field("name").map(name -> Words.of(name.text())).orElse(List.of());
        return words.containsAll(List.of("for", "each")) || words.contains("peek");
    }

    private static Set<String> declaredIn(Node iteration) {
        return iteration.selfAndDescendants()
                .flatMap(CollectionsArePipelinesRule::declaredNames)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Stream<String> declaredNames(Node node) {
        if (node.is("variable_declarator", "enhanced_for_statement", "formal_parameter", "catch_formal_parameter")) {
            return node.field("name").map(Node::text).stream();
        }
        if (node.is("lambda_expression")) {
            return node.field("parameters").stream()
                    .flatMap(parameters -> parameters.selfAndDescendants().filter(part -> part.is("identifier")))
                    .map(Node::text);
        }
        return Stream.empty();
    }

    private record Iteration(Node node, String variable) {
    }

    private record Candidate(Node body, Set<String> own) {

        // La première variable extérieure modifiée par l'itération, s'il y en a une.
        Optional<String> filledVariable() {
            return body.selfAndDescendants().flatMap(this::filled).findFirst();
        }

        private Stream<String> filled(Node node) {
            if (node.is("assignment_expression")) {
                return node.field("left").flatMap(Candidate::root).filter(this::outer).stream();
            }
            if (node.is("update_expression")) {
                return node.children().findFirst().flatMap(Candidate::root).filter(this::outer).stream();
            }
            if (node.is("method_invocation") && fills(node.field("name"))) {
                return Stream.concat(node.field("object").stream(), firstArgumentOfAStaticCall(node))
                        .flatMap(target -> root(target).filter(this::outer).stream())
                        .limit(1);
            }
            if (node.is("method_reference") && fills(node.children().reduce((first, second) -> second))) {
                return node.children().findFirst().filter(receiver -> !receiver.is("this", "super"))
                        .flatMap(Candidate::root).filter(this::outer).stream();
            }
            return Stream.empty();
        }

        // `Collections.addAll(result, ...)` : sur un type, l'accumulateur est le premier argument.
        private static Stream<Node> firstArgumentOfAStaticCall(Node call) {
            return call.field("object").filter(object -> object.is("identifier") && isType(object.text())).stream()
                    .flatMap(type -> call.field("arguments").stream().flatMap(arguments -> arguments.children().limit(1)));
        }

        private static boolean fills(Optional<Node> name) {
            return name.map(n -> Words.of(n.text()).stream().anyMatch(FILL_WORDS::contains)).orElse(false);
        }

        // `result`, `this.result`, `result[i]`, `this.cache.items` : le nom de la variable visée.
        private static Optional<String> root(Node target) {
            if (target.is("identifier")) {
                return Optional.of(target.text());
            }
            if (target.is("field_access")) {
                return target.field("object").filter(object -> object.is("this"))
                        .flatMap(object -> target.field("field").map(Node::text))
                        .or(() -> target.field("object").flatMap(Candidate::root));
            }
            if (target.is("array_access")) {
                return target.field("array").flatMap(Candidate::root);
            }
            return Optional.empty();
        }

        // Un nom en PascalCase est un type (`Collections.addAll`), pas un accumulateur ; une constante (`CACHE`) en est un.
        private boolean outer(String name) {
            return !own.contains(name) && !isType(name);
        }

        private static boolean isType(String name) {
            return Character.isUpperCase(name.charAt(0)) && !name.equals(name.toUpperCase());
        }
    }
}
