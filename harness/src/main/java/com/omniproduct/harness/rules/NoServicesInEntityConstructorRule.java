package com.omniproduct.harness.rules;

import com.omniproduct.harness.ArchConditions;
import com.omniproduct.harness.ArchFindings;
import com.omniproduct.harness.DomainTypes;
import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.List;

// HA10 (moteurs BYTECODE + AST) - une entité ne prend pas de service dans son constructeur : elle se construit
// à partir de valeurs, de value objects ou d'autres entités. Un paramètre de type service (package `..service..`
// ou nom finissant par "Service") inverserait la dépendance que l'atelier enseigne : les services dépendent des
// entités, jamais l'inverse.
public final class NoServicesInEntityConstructorRule implements HarnessRule {

    private final String modelPackage;

    // modelPackage au format ArchUnit, par exemple "..model.."
    public NoServicesInEntityConstructorRule(String modelPackage) {
        this.modelPackage = modelPackage;
    }

    @Override
    public String id() {
        return "HA10";
    }

    @Override
    public String name() {
        return "Entities must not take services in their constructor";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return ArchFindings.of(sources, ArchRuleDefinition.classes()
                .that().resideInAPackage(modelPackage)
                .and().areNotInterfaces().and().areNotEnums().and().areNotRecords()
                .should(ArchConditions.eachViolation("not take a service in their constructor", entity ->
                        entity.getConstructors().stream()
                                .filter(constructor -> DomainTypes.isEntity(entity))
                                .flatMap(constructor -> constructor.getRawParameterTypes().stream()
                                        .filter(NoServicesInEntityConstructorRule::isService)
                                        .map(service -> constructor.getDescription() + " has parameter of type <" + service.getName()
                                                + "> which looks like a service in " + constructor.getSourceCodeLocation())))));
    }

    private static boolean isService(JavaClass type) {
        return type.getPackageName().contains(".service") || type.getSimpleName().endsWith("Service");
    }
}
