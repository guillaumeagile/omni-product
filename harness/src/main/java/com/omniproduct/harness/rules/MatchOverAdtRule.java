package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.SourceTypes;
import com.omniproduct.harness.Violation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA4.5 (moteur AST) - Composable, phase 4 : le comportement de chaque alternative passe par un `switch` exhaustif
// sur l'ADT, jamais par des tests de type. Les résultats scellés (SaleDecision, PerishableCreation...) sont des ADT
// comme les autres : ils se consomment par un switch. Quatre contrôles :
//   1. pas de `instanceof` sur une ADT ni sur une de ses alternatives ;
//   2. pas de cast vers une ADT ou une alternative (`(Sellable) decision`) ;
//   3. un appel à une méthode du code qui renvoie une ADT n'est pas laissé en simple instruction (`product.canSell(today);`) :
//      son résultat est ignoré, donc aucune alternative n'est traitée ;
//   4. au moins un `switch` dont les cas (`case Perishable p ->`) couvrent toutes les alternatives d'une ADT dont les
//      alternatives sont des records. Le compilateur vérifie l'exhaustivité ; le harnais vérifie qu'elle est écrite.
public final class MatchOverAdtRule implements HarnessRule {

    private final Predicate<String> packageFilter;

    public MatchOverAdtRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA4.5";
    }

    @Override
    public String name() {
        return "ADT alternatives and sealed results are handled by an exhaustive switch: no instanceof, cast or ignored result";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        Map<String, List<String>> adts = SourceTypes.adts(sources);
        Set<String> alternatives = adts.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Set<String> records = SourceTypes.allDeclarations(sources)
                .filter(SourceTypes::isRecord)
                .map(SourceTypes::name)
                .collect(Collectors.toSet());
        Map<String, List<String>> recordAdts = adts.entrySet().stream()
                .filter(adt -> records.containsAll(adt.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<SourceFile> model = sources.files().stream().filter(file -> packageFilter.test(file.packageName())).toList();

        Set<String> sealedTypes = Stream.concat(alternatives.stream(), adts.keySet().stream()).collect(Collectors.toSet());
        Set<String> returningAdt = SourceTypes.allDeclarations(sources)
                .flatMap(SourceTypes::members)
                .filter(member -> member.is("method_declaration"))
                .filter(method -> method.field("type").map(type -> adts.containsKey(SourceTypes.typeName(type))).orElse(false))
                .flatMap(method -> method.field("name").map(Node::text).stream())
                .collect(Collectors.toSet());

        Stream<Violation> casts = model.stream()
                .flatMap(file -> file.descendants()
                        .filter(node -> node.is("cast_expression"))
                        .flatMap(node -> node.field("type").map(SourceTypes::typeName).filter(sealedTypes::contains).stream()
                                .map(type -> new Violation(file.path(), node.line(),
                                        "Cast to '" + type + "' assumes one ADT alternative. "
                                                + "Handle each alternative with one case of a switch instead (HA4.5)."))));

        Stream<Violation> ignoredResults = model.stream()
                .flatMap(file -> file.descendants()
                        .filter(node -> node.is("expression_statement"))
                        .flatMap(statement -> statement.children().filter(child -> child.is("method_invocation")))
                        .flatMap(call -> call.field("name").map(Node::text).filter(returningAdt::contains).stream()
                                .map(name -> new Violation(file.path(), call.line(),
                                        "The result of '" + name + "' is a sealed type and is ignored. "
                                                + "Switch on it and handle every alternative (HA4.5)."))));

        Stream<Violation> typeTests = model.stream()
                .flatMap(file -> file.descendants()
                        .filter(node -> node.is("instanceof_expression"))
                        .flatMap(node -> node.children()
                                .filter(child -> child.is("type_identifier", "scoped_type_identifier", "generic_type"))
                                .map(SourceTypes::typeName)
                                .filter(type -> alternatives.contains(type) || adts.containsKey(type))
                                .map(type -> new Violation(file.path(), node.line(),
                                        "'instanceof " + type + "' tests an ADT alternative. "
                                                + "Handle each alternative with one case of a switch instead (HA4.5)."))));

        boolean exhaustive = model.stream()
                .flatMap(file -> file.descendants().filter(node -> node.is("switch_expression", "switch_statement")))
                .anyMatch(switchNode -> coversAnAdt(switchNode, recordAdts));

        Stream<Violation> missingSwitch = exhaustive
                ? Stream.empty()
                : Stream.of(new Violation(
                        model.stream().findFirst().map(SourceFile::path).orElse("<no files>"), 1,
                        "No exhaustive switch over an ADT (a sealed interface whose alternatives are records): "
                                + "write one case per alternative and let the compiler check the rest."));

        return Stream.of(typeTests, casts, ignoredResults, missingSwitch).flatMap(violations -> violations).toList();
    }

    // Les cas du switch couvrent-ils toutes les alternatives d'une même ADT ?
    private static boolean coversAnAdt(Node switchNode, Map<String, List<String>> recordAdts) {
        Set<String> covered = switchNode.descendants()
                .filter(node -> node.is("type_pattern", "record_pattern"))
                .flatMap(pattern -> pattern.children().findFirst().map(SourceTypes::typeName).stream())
                .collect(Collectors.toSet());
        return recordAdts.values().stream().anyMatch(alternatives -> covered.containsAll(alternatives));
    }
}
