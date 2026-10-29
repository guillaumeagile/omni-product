package com.omniproduct.harness.fixtures.vavr.good.model;

import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.List;

public class Folder {

    public String fold(Either<String, Integer> result) {
        return result.fold(error -> "error: " + error, value -> "value: " + value);
    }

    public Option<Integer> chain(Either<String, Integer> result) {
        return result.map(value -> value + 1).toOption();
    }

    // Rendre le conteneur à l'appelant n'est pas le consommer.
    public static Option<String> parse(String text) {
        return Option.of(text).filter(value -> !value.isBlank());
    }

    public Try<Integer> attempt(String text) {
        return Try.of(() -> Integer.parseInt(text)).map(value -> value * 2);
    }

    public String foldTry(Try<Integer> attempt) {
        return attempt.fold(error -> "failed", String::valueOf);
    }

    // Les méthodes d'Object n'ouvrent rien.
    public boolean same(Option<String> left, Option<String> right) {
        return left.equals(right);
    }

    // Une liste Java garde son get(int) : seul un conteneur Vavr est visé.
    public String element(List<String> items) {
        return items.get(0);
    }

    // Le get() d'un Optional du JDK n'est pas un conteneur Vavr.
    public String optional(java.util.Optional<String> optional) {
        return optional.get();
    }
}
