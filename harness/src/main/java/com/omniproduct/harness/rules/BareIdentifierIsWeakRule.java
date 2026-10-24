package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.HarnessSeverity;
import com.omniproduct.harness.Identifiers;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA12.1 (moteur AST, avertissement seulement) - primitive obsession sur les identifiants. Tout `id` / `*Id` /
// `*Ids` (services compris) dont le type n'est pas déclaré dans le source analysé est un type JDK ou bibliothèque
// utilisé tel quel (String, int, UUID, URI...). On demande un record du domaine, pour qu'un ProductId ne puisse
// pas être confondu avec un SupplierId. Un identifiant String échoue donc HA12 ET avertit ici.
public final class BareIdentifierIsWeakRule implements HarnessRule {

    private final Predicate<String> packageFilter;

    public BareIdentifierIsWeakRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA12.1";
    }

    @Override
    public String name() {
        return "Identifiers should be domain records, not raw JDK types such as String/int/UUID (warning)";
    }

    @Override
    public HarnessSeverity severity() {
        return HarnessSeverity.WARNING;
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        Set<String> declared = sources.files().stream()
                .flatMap(SourceFile::descendants)
                .filter(node -> node.is("class_declaration", "record_declaration", "interface_declaration", "enum_declaration"))
                .flatMap(node -> node.field("name").stream())
                .map(Node::text)
                .collect(Collectors.toSet());

        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(file -> Identifiers.uses(file).flatMap(use -> Identifiers.leafTypes(use.type())
                        .map(Identifiers::simpleName)
                        .filter(type -> !declared.contains(type))
                        .map(type -> new Violation(file.path(), use.name().line(),
                                "Identifier '" + use.name().text() + "' is a raw " + type
                                        + ": declare a domain record (e.g. ProductId) wrapping it."))))
                .distinct()
                .toList();
    }
}
