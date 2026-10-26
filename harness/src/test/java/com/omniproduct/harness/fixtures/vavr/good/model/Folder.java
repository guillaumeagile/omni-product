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

    public String withDefault(Option<String> option) {
        return option.getOrElse("none");
    }

    public String computedDefault(Try<String> attempt) {
        return attempt.getOrElseGet(error -> "failed");
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
