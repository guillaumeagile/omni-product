package com.omniproduct.harness;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.function.Function;
import java.util.stream.Stream;

// Écrire une condition ArchUnit en une expression : une classe -> les messages de ses violations.
// Chaque message doit suivre le format ArchUnit ("Class <x> ... <y> in (X.java:0)") pour que
// ArchFindings sache le localiser dans le source.
public final class ArchConditions {

    private ArchConditions() {
    }

    public static ArchCondition<JavaClass> eachViolation(String description, Function<JavaClass, Stream<String>> messages) {
        return new ArchCondition<>(description) {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                messages.apply(type).forEach(message -> events.add(SimpleConditionEvent.violated(type, message)));
            }
        };
    }
}
