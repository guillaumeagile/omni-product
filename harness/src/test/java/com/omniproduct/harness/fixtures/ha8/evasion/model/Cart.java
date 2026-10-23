package com.omniproduct.harness.fixtures.ha8.evasion.model;

// Volontairement mauvais : aucun import de persistance, mais des verbes et une forme stockée qui ne sont pas les
// formes attendues (flush, reload, itemsSerialized).
public class Cart {
    private String itemsSerialized;

    public void flush() {
    }

    public void reload() {
    }
}
