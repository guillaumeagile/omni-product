package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.SourceTypes;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// HA18 (moteur AST, erreur) - un type imbriqué dans une entité est un concept à part : `Product.Warehouse`,
// `Product.Supplier` (qui entre en collision avec l'entité `Supplier`), `Supplier.Contact`, `Supplier.Address`. Il a son
// propre fichier, son propre nom, et peut être réutilisé sans importer l'entité qui l'héberge. Heuristiques :
//   - entité : une classe du package filtré qui implémente `Identifiable` (par mots, donc `Identifiable`, `IDENTIFIABLE`
//     ou `HasIdentifiable` comptent) OU qui déclare un champ d'identité (`id`, `<nom de la classe>Id`, `product_id`) ;
//   - type imbriqué : tout record, classe, interface ou enum déclaré dans le corps de l'entité, à n'importe quelle
//     profondeur, qu'il soit public, privé ou paquet, statique ou non : aucune forme n'est une exception ;
//   - hors périmètre : seules les classes entités sont ciblées ; un record (value object) qui en contient un autre
//     n'est pas jugé ici.
public final class NestedTypeInEntityRule implements HarnessRule {

    private static final Set<String> DECLARATIONS =
            Set.of("class_declaration", "record_declaration", "interface_declaration", "enum_declaration");

    private final Predicate<String> packageFilter;

    public NestedTypeInEntityRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA18";
    }

    @Override
    public String name() {
        return "A type nested in an entity is a concept of its own: give it its own file";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        Set<String> topLevel = sources.files().stream()
                .flatMap(file -> file.root().children().filter(node -> node.is(DECLARATIONS.toArray(String[]::new))))
                .map(SourceTypes::name)
                .collect(Collectors.toUnmodifiableSet());
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(file -> file.root().children()
                        .filter(node -> node.is("class_declaration") && isEntity(node))
                        .flatMap(entity -> nested(entity).map(type -> violation(file, entity, type, topLevel))))
                .toList();
    }

    private static Stream<Node> nested(Node declaration) {
        return SourceTypes.members(declaration).filter(member -> member.is(DECLARATIONS.toArray(String[]::new)))
                .flatMap(type -> Stream.concat(Stream.of(type), nested(type)));
    }

    private static boolean isEntity(Node declaration) {
        return SourceTypes.implementsIdentifiable(declaration) || declaresIdentity(declaration);
    }

    private static boolean declaresIdentity(Node declaration) {
        List<String> ownId = Stream.concat(Words.of(SourceTypes.name(declaration)).stream(), Stream.of("id")).toList();
        return SourceTypes.members(declaration).filter(member -> member.is("field_declaration"))
                .flatMap(field -> field.children().filter(child -> child.is("variable_declarator")))
                .flatMap(declarator -> declarator.field("name").stream())
                .map(name -> Words.of(name.text()))
                .anyMatch(words -> words.equals(List.of("id")) || words.equals(ownId));
    }

    private static Violation violation(SourceFile file, Node entity, Node type, Set<String> topLevel) {
        String nestedName = SourceTypes.name(type);
        String entityName = SourceTypes.name(entity);
        String collision = topLevel.contains(nestedName)
                ? " It even shares its name with the type '" + nestedName + "' declared elsewhere: two concepts, one name."
                : "";
        return new Violation(file.path(), SourceTypes.nameOf(type).line(), "'" + entityName + "." + nestedName
                + "' is declared inside the entity '" + entityName + "'." + collision
                + " Move it to its own file (" + nestedName + ".java) so it is a concept of its own, reusable without the entity.");
    }
}
