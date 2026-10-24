package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.SourceTypes;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// HA4.4 (moteur AST) - Composable, étape 3 (heuristique) : l'entrée est validée à la frontière, pas en lançant une
// exception dans un constructeur. Au moins un value object expose une méthode statique de fabrication (create, build,
// of, from, make) qui renvoie un type générique à deux arguments : le value object lui-même, et un type d'erreur déclaré
// dans le code (Either<PriceError, Price>, Result<Price, PriceError>...). Le nom de la méthode est un mot, pas une chaîne.
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
        return "A value object has a static create/build returning Either<Error, Self> (Error declared in the code)";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        Set<String> declared = SourceTypes.allDeclarations(sources).map(SourceTypes::name).collect(Collectors.toSet());

        List<SourceFile> model = sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .toList();
        boolean found = model.stream()
                .flatMap(SourceTypes::declarations)
                .filter(SourceTypes::isValueObject)
                .anyMatch(record -> hasValidatingFactory(record, declared));
        if (found) {
            return List.of();
        }
        return List.of(new Violation(
                model.stream().findFirst().map(SourceFile::path).orElse("<no files>"), 1,
                "No value object has a static create/build returning Either<Error, Self> where Error is a type of the code. "
                        + "Validate input at the boundary instead of throwing in a constructor."));
    }

    private static boolean hasValidatingFactory(Node record, Set<String> declared) {
        String self = SourceTypes.name(record);
        return SourceTypes.members(record)
                .filter(member -> member.is("method_declaration") && member.hasModifier("static"))
                .filter(method -> method.field("name").map(name -> Words.of(name.text()).stream()
                        .anyMatch(FACTORY_WORDS::contains)).orElse(false))
                .anyMatch(method -> returnsEitherOfSelfAndDeclaredError(method, self, declared));
    }

    private static boolean returnsEitherOfSelfAndDeclaredError(Node method, String self, Set<String> declared) {
        return method.field("type")
                .filter(type -> type.is("generic_type"))
                .map(SourceTypes::typeArguments)
                .filter(arguments -> arguments.size() == 2)
                .map(arguments -> arguments.stream().map(SourceTypes::typeName).toList())
                .filter(names -> names.contains(self))
                .filter(names -> names.stream().anyMatch(name -> !name.equals(self) && declared.contains(name)))
                .isPresent();
    }
}
