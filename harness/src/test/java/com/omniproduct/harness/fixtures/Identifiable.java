package com.omniproduct.harness.fixtures;

// Le marqueur d'entité des fixtures (la règle le reconnaît par son nom simple).
public interface Identifiable<T> {
    T id();
}
