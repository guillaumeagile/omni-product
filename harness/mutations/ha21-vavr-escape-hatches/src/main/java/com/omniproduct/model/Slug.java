package com.omniproduct.model;

import io.vavr.control.Either;

// Mutant HA21 : un Either ouvert avec isLeft() puis get() au lieu d'être consommé par fold.
public record Slug(String value) {
    public static Either<String, Slug> of(String value) {
        Either<String, String> checked = value == null || value.isBlank() ? Either.left("blank slug") : Either.right(value);
        return checked.isLeft() ? Either.left(checked.getLeft()) : Either.right(new Slug(checked.get()));
    }
}
