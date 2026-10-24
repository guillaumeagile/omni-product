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

// HA4.5 (moteur AST) - Composable, étape 4 : le comportement de chaque alternative passe par un `switch` exhaustif
// sur l'ADT, jamais par des tests de type. Deux contrôles :
//   1. pas de `instanceof` sur une ADT ni sur une de ses alternatives ;
//   2. au moins un `switch` dont les cas (`case Perishable p ->`) couvrent toutes les alternatives d'une ADT dont les
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
        return "ADT alternatives are handled by an exhaustive switch over records, never instanceof";
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

        return Stream.concat(typeTests, missingSwitch).toList();
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
