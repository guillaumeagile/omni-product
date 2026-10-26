package com.omniproduct.model;

// Mutant HA4.9 : une fabrique qui branche (`if`) et qui lance (`throw`) au lieu de composer. Doit compiler sans Vavr.
public record Slug(String value) {
    public static Slug of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("blank slug");
        }
        return new Slug(value);
    }
}
