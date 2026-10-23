package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// HA8 (moteur BYTECODE, ArchUnit) - un modèle de domaine ne sait pas comment il est stocké :
// aucune dépendance vers JPA (jakarta.persistence) ni Spring Data. Les repositories et la
// configuration JPA sont de l'infrastructure, hors du package du domaine.
public final class NoPersistenceInDomainRule implements HarnessRule {

    private static final Pattern LOCATION = Pattern.compile("\\((\\w+\\.java):(\\d+)\\)");

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
        JavaClasses classes = sources.classes();
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage(domainPackage)
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data..");
        EvaluationResult result = rule.evaluate(classes);

        List<Violation> violations = new ArrayList<>();
        for (String detail : result.getFailureReport().getDetails()) {
            Matcher location = LOCATION.matcher(detail);
            boolean located = location.find();
            String file = located ? location.group(1) : "?";
            int line = located ? Integer.parseInt(location.group(2)) : 0;
            violations.add(new Violation(file, line, detail));
        }
        return violations;
    }
}
