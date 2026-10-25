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

// HA4.7 (moteur AST, erreur) - une vente ne retire du stock que si le domaine l'a permise : le retrait doit
// DÉPENDRE de la décision `canSell`, pas seulement venir après elle dans le texte. Un retrait est gardé quand il se
// trouve :
//   - dans une branche d'un `if`, d'un ternaire ou d'un `switch` dont la condition porte la décision ;
//   - dans une lambda ou une référence de méthode passée à un appel dont le receveur (ou un autre argument) porte la
//     décision : `product.canSell(today).map(sellable -> withdraw(stock))`, `Option.when(canSell, () -> ...)` ;
//   - après une clause de garde : `if (decision refuse) return ...;` (ou `throw`), puis le retrait.
// Un appel `canSell` dont le résultat est ignoré, ou un contrôle dans une branche sans rapport, ne garde rien.
// Heuristiques, par mots (`Words`) :
//   - décision : appel dont les mots contiennent `can` + `sell`/`sale`, `sellable`/`saleable`, ou `sell`/`sale` +
//     `decide`/`decision`/`allowed`/`permitted` ; une variable locale initialisée par une décision la porte aussi ;
//   - retrait : appel ou référence de méthode dont un mot est withdraw, decrement, deduct, destock, unstock ; ou
//     construction/appel qui parle de stock (stock, inventory) avec une soustraction dans ses arguments (`-`, `minus`,
//     `subtract`) ; ou appel d'une méthode du même fichier qui retire, transitivement (`sellOne` -> `withdrawStock`) ;
//   - vente : méthode non privée qui retire ; un nom de vente (sell, sale, sold, buy, purchase, checkout, order) est
//     toujours contrôlé, un nom d'opération de stock (restock, discard, adjust, transfer, withdraw...) est exempté.
public final class SellChecksCanSellBeforeWithdrawRule implements HarnessRule {

    private static final Set<String> WITHDRAW_WORDS = words("withdraw", "decrement", "deduct", "destock", "unstock");
    private static final Set<String> STOCK_WORDS = words("stock", "inventory");
    private static final Set<String> SUBTRACT_WORDS = words("minus", "subtract", "decrement");
    private static final Set<String> SALE_WORDS = words("sell", "sale", "sold", "buy", "purchase", "checkout", "order");
    private static final Set<String> STOCK_OPERATION_WORDS = words("restock", "replenish", "discard", "scrap", "destroy",
            "waste", "adjust", "correct", "transfer", "move", "return", "refund", "cancel", "expire", "withdraw",
            "decrement", "deduct", "destock", "unstock");
    private static final Set<String> PERMISSION_WORDS = words("decide", "decision", "allowed", "permitted");

    private final Predicate<String> packageFilter;

    // Les mots de référence passent par le même découpage que les noms (`minus` devient `minu` au singulier).
    private static Set<String> words(String... references) {
        return Stream.of(references).flatMap(reference -> Words.of(reference).stream()).collect(Collectors.toUnmodifiableSet());
    }

    public SellChecksCanSellBeforeWithdrawRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA4.7";
    }

    @Override
    public String name() {
        return "A sale withdraws stock only when canSell allows it";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(SellChecksCanSellBeforeWithdrawRule::violations)
                .toList();
    }

    private static Stream<Violation> violations(SourceFile file) {
        List<Node> methods = file.descendants().filter(node -> node.is("method_declaration")).toList();
        Set<String> withdrawing = withdrawingMethods(methods);
        return methods.stream()
                .filter(method -> !method.hasModifier("private") && isSale(method))
                .flatMap(method -> method.field("body").stream()
                        .flatMap(body -> new Walk(withdrawing, decisionVariables(body)).unguarded(body, false))
                        .findFirst()
                        .map(withdrawal -> violation(file, method, withdrawal))
                        .stream());
    }

    private static Violation violation(SourceFile file, Node method, Node withdrawal) {
        String name = SourceTypes.name(method);
        boolean asks = method.descendants().anyMatch(SellChecksCanSellBeforeWithdrawRule::isDecisionCall);
        String problem = asks
                ? "'" + name + "' calls canSell, but the stock withdrawal does not depend on its answer."
                : "'" + name + "' withdraws stock without ever asking whether the product can be sold.";
        return new Violation(file.path(), withdrawal.line(), problem
                + " Ask the product (canSell(today)) and withdraw only where the sale is allowed: a switch on the"
                + " decision, a map on it, or a guard clause that returns the refusal first.");
    }

    // Une vente : un nom de vente est toujours contrôlé ; sinon, tout ce qui n'est pas une opération de stock nommée.
    private static boolean isSale(Node method) {
        List<String> words = Words.of(SourceTypes.name(method));
        return words.stream().anyMatch(SALE_WORDS::contains) || words.stream().noneMatch(STOCK_OPERATION_WORDS::contains);
    }

    // Point fixe : une méthode qui appelle une méthode qui retire retire aussi.
    private static Set<String> withdrawingMethods(List<Node> methods) {
        Set<String> direct = methods.stream()
                .filter(method -> method.descendants().anyMatch(node -> isDirectWithdrawal(node)))
                .map(SourceTypes::name)
                .collect(Collectors.toUnmodifiableSet());
        return Stream.iterate(direct, known -> grow(methods, known))
                .filter(known -> grow(methods, known).equals(known))
                .findFirst()
                .orElse(direct);
    }

    private static Set<String> grow(List<Node> methods, Set<String> known) {
        return Stream.concat(known.stream(), methods.stream()
                        .filter(method -> method.descendants().anyMatch(node -> isLocalCall(node, known)))
                        .map(SourceTypes::name))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isDirectWithdrawal(Node node) {
        return node.is("method_invocation", "method_reference") && nameWords(node).stream().anyMatch(WITHDRAW_WORDS::contains)
                || node.is("method_invocation", "object_creation_expression") && talksAboutStock(node) && subtracts(node);
    }

    private static boolean talksAboutStock(Node node) {
        String subject = node.is("object_creation_expression")
                ? node.field("type").map(Node::text).orElse("")
                : node.field("name").map(Node::text).orElse("") + " " + node.field("object").map(Node::text).orElse("");
        return Words.of(subject).stream().anyMatch(STOCK_WORDS::contains);
    }

    private static boolean subtracts(Node node) {
        return node.field("arguments").stream().flatMap(Node::descendants).anyMatch(arg ->
                arg.is("binary_expression") && arg.field("operator").map(Node::text).filter("-"::equals).isPresent()
                        || arg.is("method_invocation") && nameWords(arg).stream().anyMatch(SUBTRACT_WORDS::contains));
    }

    private static boolean isLocalCall(Node node, Set<String> methods) {
        return (node.is("method_invocation") && node.field("object").map(object -> object.is("this")).orElse(true)
                || node.is("method_reference") && node.children().findFirst().map(receiver -> receiver.is("this")).orElse(false))
                && methods.contains(name(node));
    }

    private static boolean isDecisionCall(Node node) {
        if (!node.is("method_invocation", "method_reference")) {
            return false;
        }
        List<String> words = nameWords(node);
        boolean aboutSale = words.contains("sell") || words.contains("sale");
        return aboutSale && (words.contains("can") || words.stream().anyMatch(PERMISSION_WORDS::contains))
                || words.contains("sellable") || words.contains("saleable");
    }

    // Variables locales qui portent la décision, directement ou par une autre variable (point fixe).
    private static Set<String> decisionVariables(Node body) {
        List<Node> declarators = body.descendants().filter(node -> node.is("variable_declarator")).toList();
        return Stream.iterate(Set.<String>of(), known -> Stream.concat(known.stream(), declarators.stream()
                                .filter(declarator -> declarator.field("value").map(value -> mentions(value, known)).orElse(false))
                                .flatMap(declarator -> declarator.field("name").map(Node::text).stream()))
                        .collect(Collectors.toUnmodifiableSet()))
                .skip(declarators.size() + 1L)
                .findFirst()
                .orElse(Set.of());
    }

    private static boolean mentions(Node node, Set<String> decisionVariables) {
        return node.selfAndDescendants().anyMatch(part -> isDecisionCall(part)
                || part.is("identifier") && decisionVariables.contains(part.text()));
    }

    private static String name(Node call) {
        return call.is("method_reference")
                ? call.children().reduce((first, second) -> second).map(Node::text).orElse("")
                : call.field("name").map(Node::text).orElse("");
    }

    private static List<String> nameWords(Node call) {
        return Words.of(name(call));
    }

    // Parcourt le corps d'une méthode en propageant « gardé par la décision » vers les branches qui en dépendent.
    private record Walk(Set<String> withdrawing, Set<String> decisionVariables) {

        Stream<Node> unguarded(Node node, boolean guarded) {
            if (isDirectWithdrawal(node) || isLocalCall(node, withdrawing)) {
                return guarded ? Stream.empty() : Stream.of(node);
            }
            if (node.is("if_statement", "ternary_expression", "switch_expression", "switch_statement")) {
                return branches(node, guarded);
            }
            if (node.is("method_invocation")) {
                return pipeline(node, guarded);
            }
            if (node.is("block", "switch_block_statement_group", "constructor_body")) {
                return statements(node.children().toList(), guarded);
            }
            return node.children().flatMap(child -> unguarded(child, guarded));
        }

        // La condition n'est pas gardée ; les branches le sont si elle porte la décision.
        private Stream<Node> branches(Node node, boolean guarded) {
            Optional<Node> condition = node.field("condition");
            boolean decided = guarded || condition.map(this::decides).orElse(false);
            return node.children().flatMap(child -> condition.filter(cond -> sameNode(cond, child)).isPresent()
                    ? unguarded(child, guarded)
                    : unguarded(child, decided));
        }

        // Une lambda ou une référence de méthode ne s'exécute que si le receveur (ou un autre argument) le permet.
        private Stream<Node> pipeline(Node call, boolean guarded) {
            List<Node> arguments = call.field("arguments").map(args -> args.children().toList()).orElse(List.of());
            boolean decided = guarded || call.field("object").map(this::decides).orElse(false)
                    || arguments.stream().anyMatch(argument -> !isDeferred(argument) && decides(argument));
            return Stream.concat(
                    call.field("object").stream().flatMap(object -> unguarded(object, guarded)),
                    arguments.stream().flatMap(argument -> unguarded(argument, guarded || isDeferred(argument) && decided)));
        }

        // Après une clause de garde (`if (refus) return ...;`), la suite du bloc est gardée.
        private Stream<Node> statements(List<Node> statements, boolean guarded) {
            List<Node> before = statements.stream().takeWhile(statement -> !isGuardClause(statement)).toList();
            List<Node> after = statements.stream().dropWhile(statement -> !isGuardClause(statement)).toList();
            return Stream.concat(
                    before.stream().flatMap(statement -> unguarded(statement, guarded)),
                    after.stream().findFirst().stream().flatMap(guard -> Stream.concat(
                            unguarded(guard, guarded),
                            after.stream().skip(1).flatMap(statement -> unguarded(statement, true)))));
        }

        private static boolean isDeferred(Node argument) {
            return argument.is("lambda_expression", "method_reference");
        }

        private static boolean sameNode(Node first, Node second) {
            return first.raw().getStartByte() == second.raw().getStartByte() && first.raw().getEndByte() == second.raw().getEndByte();
        }

        private boolean isGuardClause(Node statement) {
            return statement.is("if_statement")
                    && statement.field("condition").map(this::decides).orElse(false)
                    && statement.field("consequence").map(Walk::exits).orElse(false);
        }

        private static boolean exits(Node consequence) {
            Node last = consequence.is("block") ? consequence.children().reduce((first, second) -> second).orElse(consequence) : consequence;
            return last.is("return_statement", "throw_statement");
        }

        private boolean decides(Node node) {
            return mentions(node, decisionVariables);
        }
    }
}
