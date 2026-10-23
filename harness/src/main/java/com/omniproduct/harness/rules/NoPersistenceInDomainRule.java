package com.omniproduct.harness.rules;

import com.omniproduct.harness.ArchFindings;
import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.List;

// HA8 (moteurs BYTECODE + AST) - un modèle de domaine ne sait pas comment il est stocké :
// aucune dépendance vers JPA (jakarta.persistence) ni Spring Data. Les repositories et la
// configuration JPA sont de l'infrastructure, hors du package du domaine.
public final class NoPersistenceInDomainRule implements HarnessRule {

    private final String domainPackage;

    // domainPackage au format ArchUnit, par exemple "..model.."
    public NoPersistenceInDomainRule(String domainPackage) {
        this.domainPackage = domainPackage;
    }

    @Override
    public String id() {
        return "HA8";
    }

    @Override
    public String name() {
        return "No Persistence in Domain (no JPA / Spring Data in " + domainPackage + ")";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return ArchFindings.of(sources, ArchRuleDefinition.noClasses()
                .that().resideInAPackage(domainPackage)
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data.."));
    }
}
