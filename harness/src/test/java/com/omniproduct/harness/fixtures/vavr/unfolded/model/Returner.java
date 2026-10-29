package com.omniproduct.harness.fixtures.vavr.unfolded.model;

import io.vavr.control.Option;

// Des conteneurs construits et rendus, jamais consommés : rien n'est replié, la règle est rouge par absence.
public class Returner {

    public static Option<String> of(String text) {
        return text == null ? Option.none() : Option.of(text);
    }
}
