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
// of, from, make : des mots, donc `fromRawValue` compte) qui renvoie un résultat scellé ou une ADT de Vavr :
//   - soit une ADT dont une alternative est le value object et une autre un record d'erreur
//     (sealed interface PerishableCreation permits Perishable, InvalidDates) ;
//   - soit une ADT générique paramétrée par le value object, dont les alternatives sont des records
//     (sealed interface Validated<T> permits Valid, Invalid, renvoyée en Validated<Perishable>) ;
//   - soit une ADT de Vavr (Either, Validation, Try, Option) dont le cas de succès, le dernier argument de type, est
//     le value object (Either<InvalidDates, Perishable>, Validation<Seq<String>, Price>). Le type doit venir de
//     io.vavr : import (explicite ou `.*`) ou nom qualifié ; un `Either` maison non scellé ne compte pas.
public final class ValueObjectFactoryRule implements HarnessRule {

    private static final Set<String> FACTORY_WORDS = Set.of("create", "build", "of", "from", "make");

    private static final Set<String> VAVR_ADTS = Set.of("Either", "Validation", "Try", "Option");

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
        return "A value object has a static create/build returning a sealed result or a Vavr ADT (Either, Validation...)";
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
        boolean found = model.stream().anyMatch(file -> SourceTypes.declarations(file)
                .filter(SourceTypes::isValueObject)
                .anyMatch(record -> hasValidatingFactory(record, adts, records, imports(file))));
        if (found) {
            return List.of();
        }
        return List.of(new Violation(
                model.stream().findFirst().map(SourceFile::path).orElse("<no files>"), 1,
                "No value object has a static create/build returning a sealed result whose alternatives are the value "
                        + "object and an error record, or a Vavr ADT (Either, Validation, Try, Option) of the value object. "
                        + "Validate input at the boundary instead of throwing in a constructor."));
    }

    private static boolean hasValidatingFactory(Node record, Map<String, List<String>> adts, Set<String> records,
                                                Set<String> imports) {
        String self = SourceTypes.name(record);
        return SourceTypes.members(record)
                .filter(member -> member.is("method_declaration") && member.hasModifier("static"))
                .filter(method -> method.field("name").map(name -> Words.of(name.text()).stream()
                        .anyMatch(FACTORY_WORDS::contains)).orElse(false))
                .flatMap(method -> method.field("type").stream())
                .anyMatch(type -> isSealedResultOf(type, self, adts, records) || isVavrResultOf(type, self, imports));
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

    // Either<InvalidDates, Perishable>, io.vavr.control.Try<Perishable> : une ADT de Vavr dont le succès est le value object.
    private static boolean isVavrResultOf(Node type, String self, Set<String> imports) {
        String name = SourceTypes.typeName(type);
        List<Node> arguments = SourceTypes.typeArguments(type);
        return VAVR_ADTS.contains(name)
                && fromVavr(type, name, imports)
                && !arguments.isEmpty()
                && SourceTypes.typeName(arguments.get(arguments.size() - 1)).equals(self);
    }

    private static boolean fromVavr(Node type, String name, Set<String> imports) {
        String raw = type.children().findFirst().filter(child -> type.is("generic_type")).map(Node::text).orElse(type.text());
        return raw.startsWith("io.vavr.")
                || imports.contains("io.vavr.control." + name)
                || imports.contains("io.vavr.control.*");
    }

    // "import io.vavr.control.Either;" -> "io.vavr.control.Either" ; "import io.vavr.control.*;" -> "io.vavr.control.*"
    private static Set<String> imports(SourceFile file) {
        return file.root().children()
                .filter(node -> node.is("import_declaration"))
                .map(node -> node.text().replaceFirst("^import\\s+(static\\s+)?", "").replaceFirst("\\s*;\\s*$", ""))
                .collect(Collectors.toSet());
    }
}
