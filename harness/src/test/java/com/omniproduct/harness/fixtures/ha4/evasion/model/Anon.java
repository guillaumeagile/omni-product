package com.omniproduct.harness.fixtures.ha4.evasion.model;

// Volontairement mauvais : une sous-classe anonyme de Base est de l'héritage (HA4).
public class Anon {
    public Object make() {
        return new Base() {
        };
    }
}
