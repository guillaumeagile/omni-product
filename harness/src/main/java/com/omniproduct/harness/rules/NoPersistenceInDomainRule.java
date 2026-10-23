package com.omniproduct.harness.rules;

import com.omniproduct.harness.ArchFindings;
import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

// HA8 (moteurs BYTECODE + AST) - un modèle de domaine ne sait pas comment il est stocké. Deux heuristiques :
//  - le bytecode : aucune dépendance vers une technologie de stockage (JPA jakarta ou javax, Hibernate, Spring Data,
//    JDBC, jOOQ, MyBatis, MongoDB...), quelle que soit l'annotation ou la classe qui crée la dépendance ;
//  - l'AST : les contournements qui n'importent rien, un champ « fantôme » qui porte la forme stockée de l'objet
//    (priceJson, tagsCsv) et les méthodes qui synchronisent ou réhydratent cette forme (syncPrice, hydrateTags).
public final class NoPersistenceInDomainRule implements HarnessRule {

    private static final String[] STORAGE_PACKAGES = {
            "jakarta.persistence..", "javax.persistence..", "org.hibernate..", "org.springframework.data..",
            "org.springframework.jdbc..", "org.springframework.orm..", "java.sql..", "javax.sql..",
            "org.jooq..", "org.mybatis..", "org.apache.ibatis..", "com.mongodb.."};
    private static final Set<String> STORED_FORM_SUFFIXES = Set.of("csv", "json", "xml", "blob", "row", "serialized", "dump");
    private static final Set<String> STORAGE_VERBS =
            Set.of("sync", "hydrate", "persist", "serialize", "deserialize", "flush", "reload");

    private final String domainPackage;
    private final Predicate<String> packageFilter;

    // domainPackage au format ArchUnit, par exemple "..model.." ; packageFilter reconnaît les mêmes packages côté AST.
    public NoPersistenceInDomainRule(String domainPackage, Predicate<String> packageFilter) {
        this.domainPackage = domainPackage;
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA8";
    }

    @Override
    public String name() {
        return "No Persistence in Domain (no storage technology, stored-form fields or sync/hydrate methods in " + domainPackage + ")";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return Stream.concat(
                        ArchFindings.of(sources, ArchRuleDefinition.noClasses()
                                .that().resideInAPackage(domainPackage)
                                .should().dependOnClassesThat().resideInAnyPackage(STORAGE_PACKAGES)).stream(),
                        storedFormMembers(sources))
                .toList();
    }

    private Stream<Violation> storedFormMembers(SourceSet sources) {
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(SourceFile::descendants)
                .flatMap(node -> switch (node.type()) {
                    case "field_declaration" -> node.children().filter(child -> child.is("variable_declarator"))
                            .flatMap(declarator -> declarator.field("name").stream())
                            .filter(name -> endsWithStoredForm(name.text()))
                            .map(name -> violation(name, "field '" + name.text() + "' carries the stored form of the object (JSON/CSV/XML/blob/row)"));
                    case "method_declaration" -> node.field("name").stream()
                            .filter(name -> startsWithStorageVerb(name.text()))
                            .map(name -> violation(name, "method '" + name.text() + "' synchronises or hydrates a stored form"));
                    default -> Stream.empty();
                });
    }

    private static Violation violation(Node name, String what) {
        return new Violation(name.file().path(), name.line(),
                "Persistence concern in the domain model: " + what + ". Keep storage mapping in infrastructure.");
    }

    private static boolean endsWithStoredForm(String name) {
        List<String> words = Words.of(name);
        return words.size() > 1 && STORED_FORM_SUFFIXES.contains(words.get(words.size() - 1));
    }

    private static boolean startsWithStorageVerb(String name) {
        return STORAGE_VERBS.contains(Words.of(name).get(0));
    }
}
