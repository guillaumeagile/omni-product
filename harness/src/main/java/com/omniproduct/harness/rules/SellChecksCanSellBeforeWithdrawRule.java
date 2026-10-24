package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.HarnessSeverity;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.SourceTypes;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

// HA4.7 (moteur AST, avertissement) - dans une méthode de vente (son nom parle de `sell`), on vérifie qu'un appel
// `canSell` précède le premier retrait de stock (`withdraw`). Un retrait sans contrôle préalable décrémente le stock
// d'une vente que le domaine aurait refusée (péremption). Les mots comptent : canSell, checkCanSell, withdrawStock...
public final class SellChecksCanSellBeforeWithdrawRule implements HarnessRule {

    private final Predicate<String> packageFilter;

    public SellChecksCanSellBeforeWithdrawRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA4.7";
    }

    @Override
    public String name() {
        return "In a sell method, canSell is checked before withdraw (warning)";
    }

    @Override
    public HarnessSeverity severity() {
        return HarnessSeverity.WARNING;
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(SellChecksCanSellBeforeWithdrawRule::violations)
                .toList();
    }

    private static Stream<Violation> violations(SourceFile file) {
        return file.descendants()
                .filter(node -> node.is("method_declaration") && isSell(node))
                .flatMap(method -> {
                    List<Node> calls = method.descendants().filter(node -> node.is("method_invocation")).toList();
                    return firstWithdrawWithoutCanSell(calls).stream()
                            .map(withdraw -> new Violation(file.path(), withdraw.line(),
                                    "'" + SourceTypes.name(method) + "' withdraws stock before checking canSell. "
                                            + "Call canSell first and refuse the sale when it fails."));
                });
    }

    private static boolean isSell(Node method) {
        return Words.of(SourceTypes.name(method)).contains("sell");
    }

    // Le premier appel de retrait n'est fautif que si aucun appel de contrôle ne le précède dans l'ordre du source.
    private static Optional<Node> firstWithdrawWithoutCanSell(List<Node> calls) {
        boolean checkedBeforeWithdraw = calls.stream()
                .takeWhile(call -> !hasWord(call, "withdraw"))
                .anyMatch(call -> hasWord(call, "can") && hasWord(call, "sell"));
        return calls.stream().filter(call -> hasWord(call, "withdraw")).findFirst().filter(withdraw -> !checkedBeforeWithdraw);
    }

    private static boolean hasWord(Node call, String word) {
        return call.field("name").map(name -> Words.of(name.text()).contains(word)).orElse(false);
    }
}
