package com.omniproduct.harness.rules;

import com.omniproduct.harness.ArchConditions;
import com.omniproduct.harness.ArchFindings;
import com.omniproduct.harness.DomainTypes;
import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.List;

// HA7 (moteurs BYTECODE + AST) - fan-out, forme stricte (proxy de la philosophie Unix) : une classe du modèle
// ne référence AUCUNE autre entité. Les entités se relient par identifiant (un SupplierId), jamais en tenant
// ou en acceptant l'autre objet : ni champ, ni paramètre, ni retour, ni argument générique (List<Supplier>).
// Les value objects (records, classes sans identité) se référencent librement.
// Les classes imbriquées comptent comme leur entité englobante.
public final class FanOutRule implements HarnessRule {

    private final String modelPackage;

    // modelPackage au format ArchUnit, par exemple "..model.."
    public FanOutRule(String modelPackage) {
        this.modelPackage = modelPackage;
    }

    @Override
    public String id() {
        return "HA7";
    }

    @Override
    public String name() {
        return "Fan-out (a model class references no other entity; reference it by id instead)";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return ArchFindings.of(sources, ArchRuleDefinition.classes()
                .that().resideInAPackage(modelPackage)
                .and().areNotInterfaces().and().areNotEnums().and().areNotRecords()
                .should(ArchConditions.eachViolation("reference no other entity", type -> type.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> DomainTypes.isEntity(dependency.getTargetClass()))
                        .filter(dependency -> !DomainTypes.topLevelName(dependency.getTargetClass()).equals(DomainTypes.topLevelName(type)))
                        .map(dependency -> dependency.getDescription()))));
    }
}
