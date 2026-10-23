package com.omniproduct.harness;

import com.tngtech.archunit.core.domain.JavaClass;

// Ce que le harnais appelle une entité : une classe du modèle (pas un record, qui est un value object)
// qui a une identité, c'est-à-dire qui implémente `Identifiable` ou déclare un champ `id`.
public final class DomainTypes {

    public static final String MARKER_INTERFACE = "Identifiable";

    private DomainTypes() {
    }

    public static boolean isEntity(JavaClass type) {
        boolean isClass = !type.isInterface() && !type.isEnum() && !type.isRecord() && !type.isAnonymousClass();
        return isClass && (implementsMarker(type) || declaresId(type));
    }

    public static boolean implementsMarker(JavaClass type) {
        return type.getAllRawInterfaces().stream().anyMatch(i -> i.getSimpleName().equals(MARKER_INTERFACE));
    }

    private static boolean declaresId(JavaClass type) {
        return type.getFields().stream().anyMatch(field -> field.getName().equals("id"));
    }

    // "com.x.Product$Price" -> "com.x.Product" : une classe imbriquée appartient à son entité englobante.
    public static String topLevelName(JavaClass type) {
        String name = type.getName();
        return name.contains("$") ? name.substring(0, name.indexOf('$')) : name;
    }
}
