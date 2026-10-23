package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.UbiquitousLanguageMap;
import com.omniproduct.harness.Violation;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA6 (moteur AST) - Single Concern, la philosophie Unix : « fait une seule chose bien ». Regroupe les
// membres publics d'un type (méthodes et composants de record) par contexte borné d'après
// ubiquitous-language-map.json. Un type dont l'API publique couvre plus d'un contexte mélange des
// responsabilités : c'est le smell Product / ProductService (catalogue, prix, stock, fournisseur, transport...).
// La section "restrictions" interdit en plus, par nom de type, du vocabulaire volontairement extrait.
public final class SingleConcernRule implements HarnessRule {

    private record Member(String name, Node at) {
    }

    private final UbiquitousLanguageMap languageMap;
    private final int maxConcernsPerType;
    private final Predicate<String> packageFilter;

    public SingleConcernRule(UbiquitousLanguageMap languageMap, int maxConcernsPerType, Predicate<String> packageFilter) {
        this.languageMap = languageMap;
        this.maxConcernsPerType = maxConcernsPerType;
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA6";
    }

    @Override
    public String name() {
        return "Single Concern (public members span at most " + maxConcernsPerType + " bounded-context concern(s))";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(SourceFile::descendants)
                .filter(node -> node.is("class_declaration", "record_declaration"))
                .flatMap(type -> Stream.concat(tooManyConcerns(type), restrictedVocabulary(type)))
                .toList();
    }

    private Stream<Violation> tooManyConcerns(Node type) {
        Map<String, List<String>> byConcern = publicMembers(type)
                .flatMap(member -> languageMap.concernOf(member.name())
                        .map(concern -> Map.entry(concern, member.name())).stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, TreeMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        Node name = type.field("name").orElseThrow();
        return Stream.of(byConcern)
                .filter(concerns -> concerns.size() > maxConcernsPerType)
                .map(concerns -> new Violation(type.file().path(), name.line(),
                        "'" + name.text() + "' mixes " + concerns.size() + " concerns (max " + maxConcernsPerType + "): "
                                + concerns.entrySet().stream()
                                .map(concern -> concern.getKey() + " " + concern.getValue())
                                .collect(Collectors.joining(", ")) + ". Split by bounded context."));
    }

    private Stream<Violation> restrictedVocabulary(Node type) {
        String typeName = type.field("name").orElseThrow().text();
        return publicMembers(type)
                .flatMap(member -> languageMap.forbiddenKeyword(typeName, member.name())
                        .map(keyword -> new Violation(type.file().path(), member.at().line(),
                                "'" + typeName + "." + member.name() + "' uses '" + keyword
                                        + "' vocabulary that was deliberately extracted out of " + typeName + "."))
                        .stream());
    }

    private static Stream<Member> publicMembers(Node type) {
        Stream<Member> components = type.field("parameters").stream()
                .flatMap(Node::children)
                .flatMap(parameter -> parameter.field("name").stream())
                .map(name -> new Member(name.text(), name));
        Stream<Member> methods = type.field("body").stream()
                .flatMap(Node::children)
                .filter(member -> member.is("method_declaration") && member.hasModifier("public"))
                .flatMap(method -> method.field("name").stream())
                .map(name -> new Member(name.text(), name));
        return Stream.concat(components, methods);
    }
}
