package com.omniproduct.harness.rules;

import com.omniproduct.harness.ArchConditions;
import com.omniproduct.harness.ArchFindings;
import com.omniproduct.harness.DomainTypes;
import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

// HA7 (moteur BYTECODE) - Fan-out : une entité ne référence aucune autre entité ; elle y renvoie par identifiant.
// Une entité est un type qui implémente `Identifiable` (HA9) ou déclare un identifiant. La référence peut être un
// champ, un paramètre, un retour, un argument générique (Optional<Supplier>), ou un value object qui transporte
// l'entité (une dépendance qui fuit à travers un record : Basket -> Offer(Supplier)). Les services et les types JDK ne
// comptent pas. Une classe imbriquée compte comme son entité englobante.
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
                .and().areNotInterfaces().and().areNotEnums().and().areNotAnonymousClasses()
                .should(ArchConditions.eachViolation("reference no other entity", type -> DomainTypes.isValueRecord(type)
                        ? Stream.empty()
                        : Stream.concat(directReferences(type), throughValueObjects(type)))));
    }

    private Stream<String> directReferences(JavaClass type) {
        return type.getDirectDependenciesFromSelf().stream()
                .filter(dependency -> isOtherEntity(dependency.getTargetClass(), type))
                .map(Dependency::getDescription);
    }

    // Une référence indirecte : l'entité atteint un value object (record ou classe sans identité du modèle) qui porte
    // une autre entité. Un niveau suffit pour le harnais : c'est là que la fuite se voit dans le code.
    private Stream<String> throughValueObjects(JavaClass type) {
        return type.getDirectDependenciesFromSelf().stream()
                .map(Dependency::getTargetClass)
                .filter(target -> isValueObject(target) && !target.equals(type))
                .flatMap(valueObject -> valueObject.getDirectDependenciesFromSelf().stream()
                        .map(Dependency::getTargetClass)
                        .filter(entity -> isOtherEntity(entity, type))
                        .map(entity -> type.getDescription() + " references <" + entity.getName()
                                + "> through value object <" + valueObject.getName() + "> in " + type.getSourceCodeLocation()));
    }

    private boolean isValueObject(JavaClass target) {
        return Arrays.asList(target.getPackageName().split("\\.")).contains(modelSegment())
                && !target.isInterface() && !target.isEnum() && !target.isAnonymousClass()
                && !DomainTypes.isEntity(target);
    }

    // "..model.." -> "model" : le segment de package du modèle, quel que soit le package racine.
    private String modelSegment() {
        return modelPackage.replace(".", "");
    }

    private static boolean isOtherEntity(JavaClass target, JavaClass from) {
        return DomainTypes.isEntity(target)
                && !DomainTypes.topLevelName(target).equals(DomainTypes.topLevelName(from));
    }
}
