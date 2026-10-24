package com.omniproduct.harness;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// Les types déclarés dans les sources, vus d'un seul endroit (moteur AST). HA3 et HA4 raisonnent sur les records,
// les interfaces scellées (ADT) et leurs alternatives : ces lectures sont partagées ici plutôt que recopiées.
public final class SourceTypes {

    private static final List<String> DECLARATIONS =
            List.of("class_declaration", "record_declaration", "interface_declaration", "enum_declaration");

    private SourceTypes() {
    }

    // Toutes les déclarations de types d'un fichier, imbriquées comprises.
    public static Stream<Node> declarations(SourceFile file) {
        return file.descendants().filter(node -> DECLARATIONS.contains(node.type()));
    }

    public static Stream<Node> allDeclarations(SourceSet sources) {
        return sources.files().stream().flatMap(SourceTypes::declarations);
    }

    public static String name(Node declaration) {
        return declaration.field("name").map(Node::text).orElse("");
    }

    // Le nœud du nom : c'est là que la ligne d'une déclaration se signale (pas sur ses annotations).
    public static Node nameOf(Node declaration) {
        return declaration.field("name").orElse(declaration);
    }

    // Membres d'un type (champs, méthodes, constructeurs, types imbriqués), dans l'ordre du source.
    public static Stream<Node> members(Node declaration) {
        return declaration.field("body").stream().flatMap(Node::children);
    }

    // Composants d'un record : record Price(Money base) -> base.
    public static Stream<Node> components(Node declaration) {
        return declaration.children().filter(child -> child.is("formal_parameters")).flatMap(Node::children)
                .filter(child -> child.is("formal_parameter"));
    }

    // Le nom d'un type tel qu'on l'écrit, sans paquet ni génériques : List<Price> -> List, java.util.Optional -> Optional.
    public static String typeName(Node type) {
        return switch (type.type()) {
            case "generic_type", "array_type" -> type.children().findFirst().map(SourceTypes::typeName).orElse("");
            default -> Identifiers.simpleName(type);
        };
    }

    // Arguments de type d'un type générique : Either<E, Price> -> [E, Price].
    public static List<Node> typeArguments(Node type) {
        return type.children().filter(child -> child.is("type_arguments")).flatMap(Node::children).toList();
    }

    public static boolean isRecord(Node declaration) {
        return declaration.is("record_declaration");
    }

    public static boolean isClass(Node declaration) {
        return declaration.is("class_declaration");
    }

    public static boolean isSealed(Node declaration) {
        return declaration.hasModifier("sealed");
    }

    // Les alternatives déclarées par `sealed interface X permits A, B` : [A, B].
    public static List<String> permitted(Node declaration) {
        return declaration.children().filter(child -> child.is("permits"))
                .flatMap(Node::children).flatMap(Node::children)
                .map(SourceTypes::typeName)
                .toList();
    }

    // Un record qui porte une identité : il implémente Identifiable (le nom parle d'identité, pas seulement son orthographe).
    public static boolean implementsIdentifiable(Node declaration) {
        return declaration.children().filter(child -> child.is("super_interfaces"))
                .flatMap(Node::children).flatMap(Node::children)
                .map(SourceTypes::typeName)
                .anyMatch(name -> Words.of(name).contains("identifiable"));
    }

    // Une valeur (value object) : un record qui n'a pas d'identité, c'est-à-dire qui n'implémente pas Identifiable.
    public static boolean isValueObject(Node declaration) {
        return isRecord(declaration) && !implementsIdentifiable(declaration);
    }

    // Les ADT du code : nom de l'interface scellée -> noms de ses alternatives. Une interface scellée sans `permits`
    // n'est pas une ADT lisible par le harnais et n'est pas comptée.
    public static Map<String, List<String>> adts(SourceSet sources) {
        return allDeclarations(sources)
                .filter(declaration -> declaration.is("interface_declaration") && isSealed(declaration))
                .filter(declaration -> !permitted(declaration).isEmpty())
                .collect(java.util.stream.Collectors.toMap(SourceTypes::name, SourceTypes::permitted,
                        (first, second) -> Stream.concat(first.stream(), second.stream()).distinct().toList()));
    }
}
