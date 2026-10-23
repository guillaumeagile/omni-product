package com.omniproduct.harness.rules;

import com.omniproduct.harness.ArchConditions;
import com.omniproduct.harness.ArchFindings;
import com.omniproduct.harness.DomainTypes;
import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

// HA10 (moteurs BYTECODE + AST) - une entité ne prend pas de service dans son constructeur : elle se construit
// à partir de valeurs, de value objects ou d'autres entités. Un paramètre de type service (package `..service..`
// ou nom/annotation de collaborateur : Service, Repository, Manager, @Component...) inverserait la dépendance que l'atelier enseigne : les services dépendent des
// entités, jamais l'inverse.
public final class NoServicesInEntityConstructorRule implements HarnessRule {

    private static final Set<String> COLLABORATOR_PACKAGES =
            Set.of("service", "services", "repository", "repositories", "application", "infrastructure", "usecase", "usecases");
    private static final List<String> COLLABORATOR_SUFFIXES = List.of(
            "Service", "Repository", "Manager", "Handler", "Client", "Gateway", "Facade", "Provider", "Factory",
            "Dao", "Port", "Adapter", "Resolver", "Orchestrator", "Processor", "UseCase");
    private static final Set<String> STEREOTYPES =
            Set.of("Service", "Component", "Repository", "Controller", "RestController", "Configuration");

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
                .and().areNotInterfaces().and().areNotEnums().and().areNotAnonymousClasses()
                .should(ArchConditions.eachViolation("not take a service in their constructor", entity ->
                        entity.getConstructors().stream()
                                .filter(constructor -> DomainTypes.isEntity(entity) && !DomainTypes.isValueRecord(entity))
                                .flatMap(constructor -> constructor.getParameters().stream()
                                        .flatMap(parameter -> parameter.getType().getAllInvolvedRawTypes().stream())
                                        .filter(NoServicesInEntityConstructorRule::isService)
                                        .map(service -> constructor.getDescription() + " has parameter of type <" + service.getName()
                                                + "> which looks like a service in " + constructor.getSourceCodeLocation())))));
    }

    // Un service se reconnaît à plusieurs indices, pas au seul suffixe « Service » : son package (service, repository,
    // application, infrastructure), un suffixe de collaborateur (Manager, Handler, Client, Gateway, Facade...) ou une
    // annotation Spring (@Service, @Component, @Repository...). Les génériques comptent : Supplier<ProductService>.
    private static boolean isService(JavaClass type) {
        return lives(type) || nameSuggestsACollaborator(type) || isStereotyped(type);
    }

    private static boolean lives(JavaClass type) {
        return Arrays.stream(type.getPackageName().split("\\."))
                .anyMatch(segment -> COLLABORATOR_PACKAGES.contains(segment));
    }

    private static boolean nameSuggestsACollaborator(JavaClass type) {
        return COLLABORATOR_SUFFIXES.stream().anyMatch(type.getSimpleName()::endsWith);
    }

    private static boolean isStereotyped(JavaClass type) {
        return type.getAnnotations().stream().anyMatch(annotation -> STEREOTYPES.contains(annotation.getRawType().getSimpleName()));
    }
}
