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
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// HA4.4 (moteur AST) - Composable, étape 3 (heuristique) : l'entrée est validée à la frontière, pas en lançant une
// exception dans un constructeur. Au moins un value object expose une méthode statique de fabrication (create, build,
// of, from, make : des mots, donc `fromRawValue` compte) qui renvoie un type scellé de résultat :
//   - soit une ADT dont une alternative est le value object et une autre un record d'erreur
//     (sealed interface PerishableCreation permits Perishable, InvalidDates) ;
//   - soit une ADT générique paramétrée par le value object, dont les alternatives sont des records
//     (sealed interface Validated<T> permits Valid, Invalid, renvoyée en Validated<Perishable>).
// `Either` de Vavr n'est pas attendu ici : c'est un atout de la phase 3.
public final class ValueObjectFactoryRule implements HarnessRule {

    private static final Set<String> FACTORY_WORDS = Set.of("create", "build", "of", "from", "make");

    private final Predicate<String> packageFilter;

    public ValueObjectFactoryRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA4.4";
    }

    @Override
    public String name() {
        return "A value object has a static create/build returning a sealed result (itself or an error record)";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        Map<String, List<String>> adts = SourceTypes.adts(sources);
        Set<String> records = SourceTypes.allDeclarations(sources)
                .filter(SourceTypes::isRecord)
                .map(SourceTypes::name)
                .collect(Collectors.toSet());

        List<SourceFile> model = sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .toList();
        boolean found = model.stream()
                .flatMap(SourceTypes::declarations)
                .filter(SourceTypes::isValueObject)
                .anyMatch(record -> hasValidatingFactory(record, adts, records));
        if (found) {
            return List.of();
        }
        return List.of(new Violation(
                model.stream().findFirst().map(SourceFile::path).orElse("<no files>"), 1,
                "No value object has a static create/build returning a sealed result whose alternatives are the value "
                        + "object and an error record. Validate input at the boundary instead of throwing in a constructor."));
    }

    private static boolean hasValidatingFactory(Node record, Map<String, List<String>> adts, Set<String> records) {
        String self = SourceTypes.name(record);
        return SourceTypes.members(record)
                .filter(member -> member.is("method_declaration") && member.hasModifier("static"))
                .filter(method -> method.field("name").map(name -> Words.of(name.text()).stream()
                        .anyMatch(FACTORY_WORDS::contains)).orElse(false))
                .flatMap(method -> method.field("type").stream())
                .anyMatch(type -> isSealedResultOf(type, self, adts, records));
    }

    private static boolean isSealedResultOf(Node type, String self, Map<String, List<String>> adts, Set<String> records) {
        List<String> alternatives = adts.getOrDefault(SourceTypes.typeName(type), List.of());
        boolean selfOrError = alternatives.contains(self)
                && alternatives.stream().anyMatch(alternative -> !alternative.equals(self) && records.contains(alternative));
        boolean genericOverSelf = alternatives.size() >= 2
                && records.containsAll(alternatives)
                && SourceTypes.typeArguments(type).stream().map(SourceTypes::typeName).anyMatch(self::equals);
        return selfOrError || genericOverSelf;
    }
}
