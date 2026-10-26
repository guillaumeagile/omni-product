package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Identifiers;
import com.omniproduct.harness.Words;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.UbiquitousLanguageMap;
import com.omniproduct.harness.Violation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA6 (moteur AST) - Single Concern, la philosophie Unix : « fait une seule chose bien ». Regroupe les
// membres publics d'un type (méthodes et composants de record) par contexte borné d'après
// ubiquitous-language-map.json. Un type dont l'API publique couvre plus d'un contexte mélange des
// responsabilités : c'est le smell Product / ProductService (catalogue, prix, stock, fournisseur, transport...).
// La section "restrictions" interdit en plus, par nom de type, du vocabulaire volontairement extrait.
// Construire n'est pas une préoccupation de plus : une fabrique statique (méthode `static` qui rend le type ou son
// Builder imbriqué, sans prendre d'instance du type en paramètre) et les membres d'un Builder (ceux qui rendent le
// Builder ou le type construit) ne comptent pas dans les contextes. Le critère est la forme, jamais le nom : `of`,
// `fromSupplier` ou `ofPrice` sont acceptées au même titre, et `static Product recompute(Product p)`, qui transforme
// une instance, reste comptée. Les restrictions de vocabulaire, elles, s'appliquent à tous les membres.
public final class SingleConcernRule implements HarnessRule {

    // Un membre de l'API : son nom, l'endroit où le signaler, les types qu'il expose (retour, paramètres), et s'il
    // construit le type (fabrique statique, Builder) plutôt que de rendre un service.
    private record Member(String name, Node at, List<String> types, boolean construction) {
        Member(String name, Node at, List<String> types) {
            this(name, at, types, false);
        }
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
                .filter(member -> !member.construction())
                .flatMap(member -> concernOf(member)
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
                .flatMap(member -> forbiddenKeyword(typeName, member)
                        .map(keyword -> new Violation(type.file().path(), member.at().line(),
                                "'" + typeName + "." + member.name() + "' uses '" + keyword
                                        + "' vocabulary that was deliberately extracted out of " + typeName + "."))
                        .stream());
    }

    // Le nom d'abord ; un nom générique (get, compute, apply) se lit alors à travers les types qu'il manipule. Un type
    // d'identifiant (ProductId, SupplierId) n'appartient à aucun contexte, comme un nom d'identifiant (getId) : typer
    // l'identité d'une entité ne lui ajoute pas de préoccupation.
    private Optional<String> concernOf(Member member) {
        return languageMap.concernOf(member.name())
                .or(() -> member.types().stream()
                        .filter(type -> !Words.isIdentifier(type))
                        .flatMap(type -> languageMap.concernOf(type).stream())
                        .findFirst());
    }

    private Optional<String> forbiddenKeyword(String typeName, Member member) {
        return Stream.concat(Stream.of(member.name()), member.types().stream())
                .flatMap(vocabulary -> languageMap.forbiddenKeyword(typeName, vocabulary).stream())
                .findFirst();
    }

    // L'API visible hors de la classe : composants de record, champs publics, méthodes publiques, protégées ou de
    // package (seul `private` est caché). Un membre `static` expose aussi l'API.
    private static Stream<Member> publicMembers(Node type) {
        Stream<Member> components = type.field("parameters").stream()
                .flatMap(Node::children)
                .flatMap(parameter -> parameter.field("name").stream()
                        .map(name -> new Member(name.text(), name, typeNames(parameter.field("type").stream()))));
        Stream<Node> members = type.field("body").stream()
                .flatMap(Node::children)
                .filter(member -> !member.hasModifier("private"));
        Stream<Member> fields = members
                .filter(member -> member.is("field_declaration") && member.hasModifier("public"))
                .flatMap(field -> field.children().filter(child -> child.is("variable_declarator"))
                        .flatMap(declarator -> declarator.field("name").stream())
                        .map(name -> new Member(name.text(), name, typeNames(field.field("type").stream()))));
        return Stream.of(components, fields, methods(type)).flatMap(stream -> stream);
    }

    private static Stream<Member> methods(Node type) {
        boolean builder = isBuilder(type);
        return type.field("body").stream()
                .flatMap(Node::children)
                .filter(member -> member.is("method_declaration") && !member.hasModifier("private"))
                .flatMap(method -> method.field("name").stream().map(name -> new Member(name.text(), name,
                        typeNames(Stream.concat(method.field("type").stream(), parameterTypes(method))),
                        constructs(type, builder, method))));
    }

    // Une fabrique statique rend le type ou son Builder imbriqué, et ne prend pas d'instance du type (sinon elle le
    // transforme). Dans un Builder, construire, c'est rendre le Builder (setters fluides) ou le type construit.
    private static boolean constructs(Node type, boolean builder, Node method) {
        String own = nameOf(type);
        List<String> returned = typeNames(method.field("type").stream());
        return method.hasModifier("static")
                ? returned.stream().anyMatch(name -> name.equals(own) || nestedNames(type).contains(name))
                        && !typeNames(parameterTypes(method)).contains(own)
                : builder && returned.stream().anyMatch(name -> name.equals(own) || enclosingName(type).filter(name::equals).isPresent());
    }

    // Un Builder est un type imbriqué dont une méthode d'instance rend le type englobant (`build()`).
    private static boolean isBuilder(Node type) {
        return enclosingName(type).map(enclosing -> type.field("body").stream()
                .flatMap(Node::children)
                .filter(member -> member.is("method_declaration") && !member.hasModifier("static"))
                .anyMatch(method -> typeNames(method.field("type").stream()).contains(enclosing))).orElse(false);
    }

    private static Set<String> nestedNames(Node type) {
        return type.field("body").stream()
                .flatMap(Node::children)
                .filter(member -> member.is("class_declaration", "record_declaration"))
                .map(SingleConcernRule::nameOf)
                .collect(Collectors.toSet());
    }

    private static Optional<String> enclosingName(Node type) {
        return type.parent()
                .flatMap(Node::parent)
                .filter(outer -> outer.is("class_declaration", "record_declaration"))
                .map(SingleConcernRule::nameOf);
    }

    private static String nameOf(Node type) {
        return type.field("name").orElseThrow().text();
    }

    private static Stream<Node> parameterTypes(Node method) {
        return method.field("parameters").stream()
                .flatMap(Node::children)
                .flatMap(parameter -> parameter.field("type").stream());
    }

    private static List<String> typeNames(Stream<Node> types) {
        return types.flatMap(Identifiers::leafTypes).map(Identifiers::simpleName).toList();
    }
}
