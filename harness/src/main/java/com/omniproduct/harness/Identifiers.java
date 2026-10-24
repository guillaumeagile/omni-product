package com.omniproduct.harness;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// Repère les identifiants dans le source : tout champ, paramètre, variable locale, composant de record ou
// getter nommé `id`, `*Id` ou `*Ids`. Sert HA12 (identifiants riches) et HA12.1 (identifiants nus).
public final class Identifiers {

    // Un identifiant : son nom, son type, et la valeur qui l'initialise s'il y en a une.
    public record Use(Node name, Node type, Optional<Node> initializer) {
    }

    private static final Pattern GETTER_BY = Pattern.compile(".*By[A-Z].*");

    private Identifiers() {
    }

    // productId, ID, product_id, idProduct, supplierIds, uuid, guid : voir Words.isIdentifier.
    public static boolean isIdName(String name) {
        return Words.isIdentifier(name);
    }

    public static Stream<Use> uses(SourceFile file) {
        return file.descendants().flatMap(node -> switch (node.type()) {
            case "field_declaration", "local_variable_declaration" -> declarators(node);
            case "formal_parameter" -> use(node.field("name"), node.field("type"), Optional.empty());
            case "method_declaration" -> Stream.concat(
                    node.field("name")
                            .filter(name -> isIdGetter(name.text()))
                            .flatMap(name -> use(Optional.of(name), node.field("type"), Optional.empty()).findFirst())
                            .stream(),
                    idSetterParameter(node));
            default -> Stream.empty();
        });
    }

    // Les types réellement portés par l'identifiant : List<String> -> String, Map<K, V> -> K et V, UUID[] -> UUID.
    public static Stream<Node> leafTypes(Node type) {
        return switch (type.type()) {
            // `var` : le type est inféré de l'initialiseur, qui est vérifié par ailleurs (UUID v4, etc.).
            case "type_identifier" -> type.text().equals("var") ? Stream.empty() : Stream.of(type);
            case "generic_type" -> type.children()
                    .filter(child -> child.is("type_arguments"))
                    .flatMap(Node::children)
                    .flatMap(Identifiers::leafTypes);
            case "array_type" -> type.field("element").stream().flatMap(Identifiers::leafTypes);
            default -> Stream.of(type);
        };
    }

    public static String simpleName(Node type) {
        String text = type.text();
        return text.substring(text.lastIndexOf('.') + 1);
    }

    // setSupplierId(String value) : le paramètre d'un setter d'identifiant porte l'identifiant, quel que soit son nom.
    private static Stream<Use> idSetterParameter(Node method) {
        return method.field("name")
                .filter(name -> isIdSetter(name.text()))
                .stream()
                .flatMap(name -> method.field("parameters").stream())
                .flatMap(Node::children)
                .filter(parameter -> parameter.is("formal_parameter"))
                .flatMap(parameter -> parameter.field("name")
                        .flatMap(name -> parameter.field("type").map(type -> new Use(name, type, Optional.empty()))).stream());
    }

    private static boolean isIdSetter(String method) {
        List<String> words = Words.of(method);
        return Words.isIdentifier(method) && !words.isEmpty() && List.of("set", "with").contains(words.get(0));
    }

    private static Stream<Use> declarators(Node declaration) {
        return declaration.children()
                .filter(child -> child.is("variable_declarator"))
                .flatMap(declarator -> use(declarator.field("name"), declaration.field("type"), declarator.field("value")));
    }

    private static Stream<Use> use(Optional<Node> name, Optional<Node> type, Optional<Node> initializer) {
        return name.filter(n -> isIdName(n.text()))
                .flatMap(n -> type.map(t -> new Use(n, t, initializer)))
                .stream();
    }

    // id(), getId(), getProductId() sont des getters d'identifiant ; getById(...) est une recherche, pas un id.
    private static boolean isIdGetter(String method) {
        return method.equals("id")
                || (method.startsWith("get") && isIdName(method.substring(3)) && !GETTER_BY.matcher(method).matches());
    }
}
