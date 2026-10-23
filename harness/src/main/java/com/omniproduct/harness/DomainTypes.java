package com.omniproduct.harness;

import com.tngtech.archunit.core.domain.JavaClass;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

// Ce que le harnais appelle une entité : un type du modèle qui a une identité, c'est-à-dire qui implémente
// `Identifiable` ou déclare un champ `id` (ou `<nom>Id`, ou une annotation @Id). Un record est une entité s'il porte
// un identifiant : un record sans identifiant est un value object, exempté de HA9.
public final class DomainTypes {

    public static final String MARKER_INTERFACE = "Identifiable";

    private DomainTypes() {
    }

    public static boolean isEntity(JavaClass type) {
        boolean isType = !type.isInterface() && !type.isEnum() && !type.isAnonymousClass();
        return isType && (implementsMarker(type) || declaresId(type));
    }

    // Un record sans identité : un value object, que HA7, HA9 et HA10 ne jugent pas comme une entité.
    public static boolean isValueRecord(JavaClass type) {
        return type.isRecord() && !isEntity(type);
    }

    public static boolean implementsMarker(JavaClass type) {
        return type.getAllRawInterfaces().stream().anyMatch(i -> i.getSimpleName().equals(MARKER_INTERFACE));
    }

    // Une identité se reconnaît à un champ `id`, à un champ `<nom de la classe>Id` (productId dans Product, ProductID,
    // product_id) ou à une annotation d'identité (@Id, @EmbeddedId), pas seulement au nom exact `id`.
    private static boolean declaresId(JavaClass type) {
        List<String> ownId = Stream.concat(Words.of(type.getSimpleName()).stream(), Stream.of("id")).toList();
        return type.getFields().stream().anyMatch(field -> Words.of(field.getName()).equals(List.of("id"))
                || Words.of(field.getName()).equals(ownId)
                || field.getAnnotations().stream().anyMatch(a -> Set.of("Id", "EmbeddedId").contains(a.getRawType().getSimpleName())));
    }

    // "com.x.Product$Price" -> "com.x.Product" : une classe imbriquée appartient à son entité englobante.
    public static String topLevelName(JavaClass type) {
        String name = type.getName();
        return name.contains("$") ? name.substring(0, name.indexOf('$')) : name;
    }
}
