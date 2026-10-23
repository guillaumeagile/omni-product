package com.omniproduct.harness.rules;

import com.omniproduct.harness.ArchConditions;
import com.omniproduct.harness.ArchFindings;
import com.omniproduct.harness.DomainTypes;
import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.List;
import java.util.stream.Stream;

// HA9 (moteurs BYTECODE + AST) - les entités implémentent Identifiable : « est-ce une entité ? » n'a qu'une
// seule réponse. Toute classe du package du modèle doit implémenter `Identifiable` (reconnu par son nom
// simple). Les records sont les value objects du modèle (immuables, sans identité) et en sont exemptés,
// de même que les interfaces et les enums.
public final class EntityMustImplementIdentifiableRule implements HarnessRule {

    private static final String MARKER_INTERFACE = DomainTypes.MARKER_INTERFACE;

    private final String modelPackage;

    // modelPackage au format ArchUnit, par exemple "..model.."
    public EntityMustImplementIdentifiableRule(String modelPackage) {
        this.modelPackage = modelPackage;
    }

    @Override
    public String id() {
        return "HA9";
    }

    @Override
    public String name() {
        return "Entities must implement " + MARKER_INTERFACE + " (classes in " + modelPackage + ", records are value objects)";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return ArchFindings.of(sources, ArchRuleDefinition.classes()
                .that().resideInAPackage(modelPackage)
                .and().areNotInterfaces().and().areNotEnums().and().areNotRecords().and().areNotAnonymousClasses()
                .should(ArchConditions.eachViolation("implement " + MARKER_INTERFACE, type -> Stream.of(type)
                        .filter(t -> !DomainTypes.implementsMarker(t))
                        .map(t -> t.getDescription() + " does not implement <" + MARKER_INTERFACE + "> in " + t.getSourceCodeLocation()))));
    }
}
