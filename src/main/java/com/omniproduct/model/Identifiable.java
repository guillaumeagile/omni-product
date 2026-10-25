package com.omniproduct.model;

// Une entité a une identité. Les records du modèle sont des objets de valeur, sans identité, et n'implémentent pas ce marqueur.
public interface Identifiable {

    String getId();
}
