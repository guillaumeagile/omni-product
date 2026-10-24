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

// HA4.6 (moteur BYTECODE) - le modèle ne lit jamais l'horloge : il reçoit `today`, ou un Clock injecté, pour que chaque
// cas de date se teste sans attendre. Un appel est une lecture de l'horloge s'il vient du JDK (java.time, Date,
// Calendar, System) et que son nom parle de temps courant : now, systemUTC, currentTimeMillis, nanoTime... ou un
// constructeur `new Date()` sans argument. Le bytecode voit aussi les imports statiques et les alias, que l'AST rate.
public final class NoClockInModelRule implements HarnessRule {

    // java.time, Date, Calendar : des fabriques qui lisent l'heure courante (now, systemUTC, Calendar.getInstance...).
    private static final Set<String> CLOCK_WORDS = Set.of("now", "today", "clock", "utc", "system", "instance");
    // System : currentTimeMillis et nanoTime. Hors de System, « millis » est une conversion de durée (Duration.toMillis).
    private static final Set<String> SYSTEM_CLOCK_WORDS = Set.of("milli", "nano");

    private final String[] packages;

    // packages au format ArchUnit, par exemple "..model.."
    public NoClockInModelRule(String... packages) {
        this.packages = packages;
    }

    @Override
    public String id() {
        return "HA4.6";
    }

    @Override
    public String name() {
        return "The model never reads the clock: pass today, or inject a Clock";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return ArchFindings.of(sources, ArchRuleDefinition.classes()
                .that().resideInAnyPackage(packages)
                .should(notReadTheClock()));
    }

    private static ArchCondition<JavaClass> notReadTheClock() {
        return ArchConditions.eachViolation("not read the clock", type -> Stream.concat(
                        type.getMethodCallsFromSelf().stream()
                                .filter(call -> readsTheClock(call.getTarget().getOwner().getName(), call.getTarget().getName(), call.getTarget().getParameterTypes().size()))
                                .map(call -> describe(call.getOrigin(), call.getTarget().getOwner().getName() + "." + call.getTarget().getName())),
                        // ArchUnit range les appels de constructeur à part : new Date() n'apparaît que là.
                        type.getConstructorCallsFromSelf().stream()
                                .filter(call -> readsTheClock(call.getTarget().getOwner().getName(), call.getTarget().getName(), call.getTarget().getParameterTypes().size()))
                                .map(call -> describe(call.getOrigin(), call.getTarget().getOwner().getName() + ".<init>")))
                .map(message -> message + " in " + type.getSourceCodeLocation()));
    }

    private static String describe(JavaCodeUnit origin, String target) {
        return "Method <" + origin.getFullName() + "> reads the clock with <" + target + ">";
    }

    private static boolean readsTheClock(String owner, String method, int parameters) {
        List<String> words = Words.of(method);
        boolean systemClock = owner.equals("java.lang.System") && words.stream().anyMatch(SYSTEM_CLOCK_WORDS::contains);
        boolean timeApi = (owner.startsWith("java.time.") || owner.equals("java.util.Calendar"))
                && words.stream().anyMatch(CLOCK_WORDS::contains);
        boolean dateConstructor = method.equals("<init>") && owner.equals("java.util.Date") && parameters == 0;
        return systemClock || timeApi || dateConstructor;
    }
}
