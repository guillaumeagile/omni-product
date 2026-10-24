package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Identifiers;
import com.omniproduct.harness.Identifiers.Use;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.UbiquitousLanguageMap;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

// HA11 (moteur AST) - obsession pour les types primitifs, restreinte aux concepts du domaine. Un membre du modèle
// (champ, paramètre, composant de record, retour de méthode) dont le nom désigne un concept du vocabulaire
// (ubiquitous-language-map.json) ne porte pas une valeur brute (String, int, long, BigDecimal...) : il lui faut un
// type du domaine, un record qui enveloppe la valeur (record Price(BigDecimal amount)).
// Étroite par choix : un nom générique (name, description) n'est pas concerné, un nom que seul le contexte faible
// Lifecycle reconnaît non plus, et les identifiants (id, *Id, sku, siren...) relèvent de HA12.
public final class PrimitiveObsessionRule implements HarnessRule {

    // Valeurs brutes du JDK qui portent une grandeur ou un texte métier.
    private static final Set<String> RAW_VALUES = Set.of("String", "int", "long", "short", "byte", "float", "double",
            "Integer", "Long", "Short", "Byte", "Float", "Double", "BigDecimal", "BigInteger");

    private final UbiquitousLanguageMap language;
    private final Predicate<String> packageFilter;

    public PrimitiveObsessionRule(UbiquitousLanguageMap language, Predicate<String> packageFilter) {
        this.language = language;
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA11";
    }

    @Override
    public String name() {
        return "Domain concepts are not raw primitives: wrap them in a domain type (record)";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(this::rawConcepts)
                .distinct()
                .toList();
    }

    private Stream<Violation> rawConcepts(SourceFile file) {
        return members(file)
                .filter(member -> !Words.isIdentifier(member.name().text()))
                .flatMap(member -> concept(member.name().text()).stream()
                        .flatMap(concept -> Identifiers.leafTypes(member.type())
                                .map(Identifiers::simpleName)
                                .filter(RAW_VALUES::contains)
                                .map(type -> new Violation(file.path(), member.name().line(),
                                        "Member '" + member.name().text() + "' (" + concept + ") is a raw " + type
                                                + ": wrap it in a domain type, e.g. a record holding the " + type + "."))));
    }

    // Le contexte faible (Lifecycle) ne suffit pas à désigner un concept : sans autre mot du vocabulaire, le nom est libre.
    private Optional<String> concept(String memberName) {
        return language.concernOf(memberName).filter(concern -> !language.weakConcerns().contains(concern));
    }

    // Champs (y compris de chaque déclarateur), paramètres et composants de record, retours de méthode.
    private static Stream<Use> members(SourceFile file) {
        return file.descendants().flatMap(node -> switch (node.type()) {
            case "field_declaration" -> node.children()
                    .filter(child -> child.is("variable_declarator"))
                    .flatMap(declarator -> member(declarator.field("name"), node.field("type")));
            case "formal_parameter", "method_declaration" -> member(node.field("name"), node.field("type"));
            default -> Stream.empty();
        });
    }

    private static Stream<Use> member(Optional<Node> name, Optional<Node> type) {
        return name.flatMap(n -> type.map(t -> new Use(n, t, Optional.empty()))).stream();
    }
}
