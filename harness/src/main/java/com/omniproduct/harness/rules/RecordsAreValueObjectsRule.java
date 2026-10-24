package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.SourceTypes;
import com.omniproduct.harness.Violation;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

// HA3 (moteur AST) - les records sont des value objects, jamais des entités : égalité par valeur, pas d'identité.
// Trois contrôles :
//   1. un record n'implémente pas Identifiable (HA9 : l'identité appartient aux entités) ;
//   2. le package de value objects (`valueobject`, au singulier ou au pluriel) ne contient que des records,
//      pas de classe ;
//   3. le record `Price` existe dans ce package : c'est le livrable de l'exercice (extraire la TVA de Price en
//      gardant un Price composable et immuable).
// L'immutabilité (setters, collections mutables) relève de HA2.
public final class RecordsAreValueObjectsRule implements HarnessRule {

    static final String VALUE_OBJECT_PACKAGE_WORD = "valueobject";
    static final String REQUIRED_VALUE_OBJECT = "Price";

    private final Predicate<String> packageFilter;

    public RecordsAreValueObjectsRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA3";
    }

    @Override
    public String name() {
        return "Records are value objects, never entities; the value-object package holds records only; 'Price' must be one";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        List<SourceFile> model = sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .toList();

        Stream<Violation> identities = model.stream()
                .flatMap(SourceTypes::declarations)
                .filter(SourceTypes::isRecord)
                .filter(SourceTypes::implementsIdentifiable)
                .map(record -> at(record, "'" + SourceTypes.name(record) + "' is a record that implements Identifiable. "
                        + "Records are value objects, not entities: drop Identifiable, or make it a class that follows HA9/HA10."));

        Stream<Violation> classesInValueObjectPackage = model.stream()
                .filter(file -> isValueObjectPackage(file.packageName()))
                .flatMap(SourceTypes::declarations)
                .filter(SourceTypes::isClass)
                .map(type -> at(type, "'" + SourceTypes.name(type) + "' is a class in the value-object package. "
                        + "Only records belong there: make it a record."));

        Stream<Violation> missingPrice = model.stream()
                .filter(file -> isValueObjectPackage(file.packageName()))
                .flatMap(SourceTypes::declarations)
                .anyMatch(type -> SourceTypes.isRecord(type) && REQUIRED_VALUE_OBJECT.equals(SourceTypes.name(type)))
                ? Stream.empty()
                : Stream.of(new Violation(
                        model.stream().findFirst().map(SourceFile::path).orElse("<no files>"), 1,
                        "No record named '" + REQUIRED_VALUE_OBJECT + "' in a value-object package. "
                                + "'" + REQUIRED_VALUE_OBJECT + "' must be a record value object (see CUPID-step1-2.md)."));

        return Stream.of(identities, classesInValueObjectPackage, missingPrice)
                .flatMap(violations -> violations)
                .toList();
    }

    // com.omniproduct.model.valueobject, com.omniproduct.model.value_objects, valueObjects... : un segment qui commence
    // par « valueobject » une fois ramené en minuscules sans séparateur.
    static boolean isValueObjectPackage(String packageName) {
        return Arrays.stream(packageName.split("\\."))
                .map(segment -> segment.toLowerCase().replaceAll("[^a-z]", ""))
                .anyMatch(segment -> segment.startsWith(VALUE_OBJECT_PACKAGE_WORD));
    }

    static Violation at(Node node, String message) {
        return new Violation(node.file().path(), SourceTypes.nameOf(node).line(), message);
    }
}
