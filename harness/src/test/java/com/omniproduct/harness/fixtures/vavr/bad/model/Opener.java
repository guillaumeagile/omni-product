package com.omniproduct.harness.fixtures.vavr.bad.model;

import io.vavr.Value;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.function.Function;

import static io.vavr.control.Either.right;

public class Opener {

    public String get(Either<String, String> result) {
        return result.get();
    }

    public String left(Either<String, String> result) {
        return result.getLeft();
    }

    public boolean isLeft(Either<String, String> result) {
        return result.isLeft();
    }

    public boolean isRight(Either<String, String> result) {
        return result.isRight();
    }

    public String orElseThrow(Option<String> option) {
        return option.getOrElseThrow(IllegalStateException::new);
    }

    public boolean isEmpty(Option<String> option) {
        return option.isEmpty();
    }

    public Throwable cause(Try<String> attempt) {
        return attempt.getCause();
    }

    public boolean failed(Try<String> attempt) {
        return attempt.isFailure();
    }

    // Typé par le supertype : l'appel vise io.vavr.Value.
    public String viaValue(Value<String> value) {
        return value.get();
    }

    // Référence de méthode : pas d'appel visible dans le corps.
    public Function<Either<String, String>, String> reference() {
        return Either::get;
    }

    // Import statique et chaîne : l'appel est le même.
    public String chained() {
        return right("x").swap().getLeft();
    }
}
