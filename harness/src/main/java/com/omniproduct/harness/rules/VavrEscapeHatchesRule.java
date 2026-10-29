package com.omniproduct.harness.rules;

import com.omniproduct.harness.ArchConditions;
import com.omniproduct.harness.ArchFindings;
import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;
import com.tngtech.archunit.core.domain.AccessTarget;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.JavaCodeUnitReference;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

// HA21 (moteur BYTECODE) - un `Either`, `Option`, `Try` ou `Validation` de Vavr se consomme par `fold`, et seulement
// par `fold` : c'est la seule sortie qui oblige à traiter les deux cas. Liste blanche, pas liste noire : un appel (ou
// une référence de méthode) sur un conteneur (paquet `io.vavr.control`, ou `io.vavr.Value` dont ils héritent) dont le
// type de retour n'est plus un conteneur de `io.vavr.control` fait sortir la valeur du conteneur. Hors `fold`, toute
// sortie est refusée, quel que soit son nom : `get()`, `getOrNull()`, `getOrElse(v)`, `isEmpty()`, `contains(x)`,
// `toJavaOptional()`, `toList()`, `iterator()`, `forEach(...)`. Transformer en restant dans le conteneur (`map`,
// `flatMap`, `filter`, `toEither`, `Option.of`) et rendre le conteneur à l'appelant restent permis. Les méthodes
// d'Object (`equals`, `hashCode`, `toString`) n'ouvrent rien. Rouge par absence, comme HA15 : tant qu'aucun `fold`
// n'existe dans le périmètre, il n'y a rien de consommé à juger, et une règle verte par vide ne prouve rien.
public final class VavrEscapeHatchesRule implements HarnessRule {

    private static final String CONTROL = "io.vavr.control.";
    private static final Set<String> OBJECT_METHODS = Set.of("equals", "hashCode", "toString");

    private final String[] packages;

    // packages au format ArchUnit, par exemple "..model.."
    public VavrEscapeHatchesRule(String... packages) {
        this.packages = packages;
    }

    @Override
    public String id() {
        return "HA21";
    }

    @Override
    public String name() {
        return "A Vavr container (Either, Option, Try, Validation) is consumed by fold, and by nothing else";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        List<JavaClass> scope = sources.classes().stream()
                .filter(JavaClass.Predicates.resideInAnyPackage(packages))
                .toList();
        List<Violation> exits = ArchFindings.of(sources, ArchRuleDefinition.classes()
                .that().resideInAnyPackage(packages)
                .should(consumeVavrContainersByFold())
                .allowEmptyShould(true));
        return Stream.concat(exits.stream(), absence(scope)).toList();
    }

    private static ArchCondition<JavaClass> consumeVavrContainersByFold() {
        return ArchConditions.eachViolation("consume Vavr containers by fold", type -> accesses(type)
                .filter(access -> leaves(access.getTarget()) && !isFold(access.getTarget()))
                .map(access -> "Method <" + access.getOrigin().getFullName() + "> takes a value out of a Vavr container with <"
                        + access.getTarget().getOwner().getSimpleName() + separator(access) + access.getTarget().getName()
                        + ">: consume it with fold, which handles both cases in " + access.getSourceCodeLocation()));
    }

    // Rien de consommé, rien à juger : la règle reste rouge tant qu'aucun conteneur n'est replié par fold.
    private static Stream<Violation> absence(List<JavaClass> scope) {
        if (scope.stream().flatMap(VavrEscapeHatchesRule::accesses).anyMatch(access -> isFold(access.getTarget()))) {
            return Stream.empty();
        }
        // Montrer la première classe (par nom, pour rester déterministe) qui manipule un conteneur, sinon la première.
        List<JavaClass> sorted = scope.stream().sorted(Comparator.comparing(JavaClass::getName)).toList();
        return Stream.of(new Violation(
                sorted.stream()
                        .filter(type -> accesses(type).anyMatch(access -> isContainer(access.getTarget().getOwner().getName())))
                        .findFirst()
                        .or(() -> sorted.stream().findFirst())
                        .map(type -> type.getSourceCodeLocation().getSourceFileName())
                        .orElse("<no classes>"),
                1,
                "No Vavr container (Either, Option, Try, Validation) is consumed by fold in model or service. Where a "
                        + "result may have failed, fold it: result.fold(whenFailed, whenSucceeded)."));
    }

    private static Stream<JavaCodeUnitAccess<?>> accesses(JavaClass type) {
        return Stream.<JavaCodeUnitAccess<?>>concat(type.getMethodCallsFromSelf().stream(), type.getMethodReferencesFromSelf().stream());
    }

    private static String separator(JavaCodeUnitAccess<?> access) {
        return access instanceof JavaCodeUnitReference<?> ? "::" : ".";
    }

    // Une sortie : un membre d'un conteneur Vavr dont le retour quitte io.vavr.control.
    private static boolean leaves(AccessTarget.CodeUnitAccessTarget target) {
        return isContainer(target.getOwner().getName())
                && !OBJECT_METHODS.contains(target.getName())
                && !target.getRawReturnType().getName().startsWith(CONTROL);
    }

    private static boolean isFold(AccessTarget.CodeUnitAccessTarget target) {
        return isContainer(target.getOwner().getName()) && Words.of(target.getName()).equals(List.of("fold"));
    }

    private static boolean isContainer(String owner) {
        return owner.startsWith(CONTROL) || owner.equals("io.vavr.Value");
    }
}
