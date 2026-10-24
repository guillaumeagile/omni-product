package com.omniproduct.harness.rules;

import com.omniproduct.harness.ArchConditions;
import com.omniproduct.harness.ArchFindings;
import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.List;
import java.util.Set;

// HA4.1 (moteur BYTECODE) - Composable : on étend par composition, pas par héritage de classe.
// Toute classe qui dérive d'une autre classe est signalée. Implémenter des interfaces est permis,
// de même que les types que Java impose de dériver : Object, Record, Enum et les exceptions.
public final class NoInheritanceRule implements HarnessRule {

    private static final Set<String> IMPLICIT_SUPERCLASSES =
            Set.of(Object.class.getName(), Record.class.getName(), Enum.class.getName());

    private final String[] packages;

    // packages au format ArchUnit, par exemple "..model..", "..service.."
    public NoInheritanceRule(String... packages) {
        this.packages = packages;
    }

    @Override
    public String id() {
        return "HA4.1";
    }

    @Override
    public String name() {
        return "Composable (no class inheritance, only interface implementation) in " + String.join(", ", packages);
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return ArchFindings.of(sources, ArchRuleDefinition.classes()
                .that().resideInAnyPackage(packages)
                .and().areNotInterfaces()
                .should(notExtendAnotherClass()));
    }

    private static ArchCondition<JavaClass> notExtendAnotherClass() {
        return ArchConditions.eachViolation("not extend another class", type -> type.getRawSuperclass().stream()
                .filter(superclass -> !IMPLICIT_SUPERCLASSES.contains(superclass.getName()))
                .filter(superclass -> !type.isAssignableTo(Throwable.class))
                .map(superclass -> type.getDescription() + " extends <" + superclass.getName() + "> in " + type.getSourceCodeLocation()));
    }
}
