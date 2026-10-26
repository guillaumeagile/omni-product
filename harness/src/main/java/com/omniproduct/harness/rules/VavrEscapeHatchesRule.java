package com.omniproduct.harness.rules;

import com.omniproduct.harness.ArchConditions;
import com.omniproduct.harness.ArchFindings;
import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

// HA21 (moteur BYTECODE) - un `Either`, `Option`, `Try` ou `Validation` de Vavr se consomme par `fold` (ou `map`,
// `flatMap`, `getOrElse(valeur)`), jamais en l'ouvrant : `get()`, `getLeft()`, `getOrElseThrow()` lancent quand on
// s'est trompé de côté, et `isLeft()`, `isRight()`, `isEmpty()`... ne font que rejouer le `if` que Vavr évite. Un appel
// est une porte dérobée s'il vise le paquet `io.vavr.control` (ou `io.vavr.Value`, dont ils héritent `get`) et que son
// nom, lu en mots, ouvre le conteneur : `get` sans argument, `get` + left/error/cause, tout mot `throw`, ou `is` +
// left/right/empty/defined/success/failure/valid/invalid. Le bytecode voit aussi les références de méthode
// (`Either::get`), les imports statiques et les variables typées par un supertype.
public final class VavrEscapeHatchesRule implements HarnessRule {

    private static final Set<String> OPENING_WORDS = Set.of("left", "error", "cause");
    private static final Set<String> TEST_WORDS = Set.of("left", "right", "empty", "defined", "present", "success", "failure", "valid", "invalid");

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
        return "A Vavr Either/Option is consumed by fold, never opened with get, getLeft, isLeft or getOrElseThrow";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return ArchFindings.of(sources, ArchRuleDefinition.classes()
                .that().resideInAnyPackage(packages)
                .should(notOpenAVavrContainer()));
    }

    private static ArchCondition<JavaClass> notOpenAVavrContainer() {
        return ArchConditions.eachViolation("not open a Vavr container", type -> Stream.concat(
                        type.getMethodCallsFromSelf().stream()
                                .filter(call -> opens(call.getTarget().getOwner().getName(), call.getTarget().getName(), call.getTarget().getParameterTypes().size()))
                                .map(call -> describe(call.getOrigin(), call.getTarget().getOwner().getSimpleName() + "." + call.getTarget().getName())),
                        type.getMethodReferencesFromSelf().stream()
                                .filter(reference -> opens(reference.getTarget().getOwner().getName(), reference.getTarget().getName(), reference.getTarget().getParameterTypes().size()))
                                .map(reference -> describe(reference.getOrigin(), reference.getTarget().getOwner().getSimpleName() + "::" + reference.getTarget().getName())))
                .map(message -> message + " in " + type.getSourceCodeLocation()));
    }

    private static String describe(JavaCodeUnit origin, String target) {
        return "Method <" + origin.getFullName() + "> opens a Vavr container with <" + target
                + ">: consume it with fold, map, flatMap or getOrElse(value)";
    }

    private static boolean opens(String owner, String method, int parameters) {
        List<String> words = Words.of(method);
        boolean vavrContainer = owner.startsWith("io.vavr.control.") || owner.equals("io.vavr.Value");
        boolean throwing = words.contains("throw");
        boolean get = parameters == 0 && words.equals(List.of("get"));
        boolean getSide = parameters == 0 && words.size() > 1 && words.get(0).equals("get") && words.stream().skip(1).anyMatch(OPENING_WORDS::contains);
        boolean test = parameters == 0 && words.size() == 2 && words.get(0).equals("is") && TEST_WORDS.contains(words.get(1));
        return vavrContainer && (throwing || get || getSide || test);
    }
}
