package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA16 (moteur AST, erreur) - une décision du domaine se compose, elle ne se teste pas : un `if` (ou
// un ternaire retourné) dont la condition porte une décision `can*` et dont une branche sort de la méthode par un
// `return` ou un `throw` coupe le flux. `Option.when(canSell, ...)`, `filter`, `map` ou `fold` en font une étape.
// Heuristiques, par mots (`Words`) :
//   - décision : appel ou référence de méthode dont les mots contiennent `can`, ou `sellable`/`saleable`/`allowed`/
//     `permitted`/`eligible`/`decide`/`decision` ; une variable locale initialisée par une décision la porte aussi,
//     directement ou par une autre variable (point fixe) ; la négation, `&&`, `||` n'y changent rien ;
//   - branche qui sort : `return` ou `throw` dans la branche `then` ou `else`, hors lambda et classe anonyme
//     (un `return` de lambda ne sort pas de la méthode) ; une branche sans sortie (`if (canX) counter.reset()`) n'est
//     pas une garde, et un `switch` sur la décision relève de HA4.5 ;
//   - périmètre : le package filtré (`service`), toutes méthodes, privées comprises.
public final class DecisionComposesRule implements HarnessRule {

    private static final Set<String> DECISION_WORDS =
            Set.of("can", "sellable", "saleable", "allowed", "permitted", "eligible", "decide", "decision");

    private final Predicate<String> packageFilter;

    public DecisionComposesRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA16";
    }

    @Override
    public String name() {
        return "A domain decision is composed (Option.when, filter, map, fold), not tested by an if that returns";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(DecisionComposesRule::violations)
                .toList();
    }

    private static Stream<Violation> violations(SourceFile file) {
        return file.descendants()
                .filter(node -> node.is("method_declaration", "constructor_declaration"))
                .flatMap(method -> {
                    Set<String> variables = decisionVariables(method);
                    return method.descendants()
                            .filter(node -> isBranchingOnDecision(node, variables))
                            .map(node -> violation(file, method, node));
                });
    }

    private static boolean isBranchingOnDecision(Node node, Set<String> variables) {
        return node.is("if_statement") && mentionsDecision(node.field("condition"), variables)
                && Stream.of("consequence", "alternative").flatMap(branch -> node.field(branch).stream()).anyMatch(DecisionComposesRule::exits)
                || node.is("ternary_expression") && mentionsDecision(node.field("condition"), variables)
                && node.parent().map(parent -> parent.is("return_statement")).orElse(false);
    }

    private static boolean mentionsDecision(Optional<Node> condition, Set<String> variables) {
        return condition.map(cond -> mentions(cond, variables)).orElse(false);
    }

    private static boolean mentions(Node node, Set<String> variables) {
        return node.selfAndDescendants().anyMatch(part -> isDecisionCall(part)
                || part.is("identifier") && variables.contains(part.text()));
    }

    private static boolean isDecisionCall(Node node) {
        return node.is("method_invocation", "method_reference")
                && Words.of(methodName(node)).stream().anyMatch(DECISION_WORDS::contains);
    }

    private static String methodName(Node call) {
        return call.is("method_reference")
                ? call.children().reduce((first, second) -> second).map(Node::text).orElse("")
                : call.field("name").map(Node::text).orElse("");
    }

    // Une branche sort de la méthode quand elle contient un return/throw qui n'est pas celui d'une lambda.
    private static boolean exits(Node branch) {
        return branch.is("return_statement", "throw_statement")
                || !branch.is("lambda_expression", "class_body") && branch.children().anyMatch(DecisionComposesRule::exits);
    }

    private static Set<String> decisionVariables(Node method) {
        List<Node> declarators = method.descendants().filter(node -> node.is("variable_declarator")).toList();
        return Stream.iterate(Set.<String>of(), known -> Stream.concat(known.stream(), declarators.stream()
                                .filter(declarator -> declarator.field("value").map(value -> mentions(value, known)).orElse(false))
                                .flatMap(declarator -> declarator.field("name").map(Node::text).stream()))
                        .collect(Collectors.toUnmodifiableSet()))
                .skip(declarators.size() + 1L)
                .findFirst()
                .orElse(Set.of());
    }

    private static Violation violation(SourceFile file, Node method, Node branching) {
        String name = method.field("name").map(Node::text).orElse("?");
        return new Violation(file.path(), branching.line(), "'" + name + "' tests a domain decision (can...) with "
                + (branching.is("if_statement") ? "an if that returns" : "a ternary") + ". Compose it instead:"
                + " Option.when(decision, () -> ...), or filter/map/fold on the result, so the refusal is a step, not a branch.");
    }
}
