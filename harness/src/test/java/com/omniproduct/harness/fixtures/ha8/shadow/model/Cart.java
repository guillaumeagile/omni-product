package com.omniproduct.harness.fixtures.ha8.shadow.model;

// Volontairement mauvais : aucune annotation ni import de persistance, mais le modèle porte sa forme stockée (HA8).
public class Cart {
    private String itemsJson;
    private String tags_csv;

    public void syncItems() {
    }

    public void hydrateTags() {
    }
}
