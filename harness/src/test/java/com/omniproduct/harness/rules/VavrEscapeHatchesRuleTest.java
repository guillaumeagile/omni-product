package com.omniproduct.harness.rules;

import com.omniproduct.harness.Fixtures;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VavrEscapeHatchesRuleTest {

    private final VavrEscapeHatchesRule rule = new VavrEscapeHatchesRule("..model..");

    @Test
    void flagsEveryWayOfOpeningAVavrContainer() {
        List<Violation> violations = rule.check(Fixtures.load("vavr.bad"));

        assertThat(violations).extracting(Violation::message)
                .anySatisfy(message -> assertThat(message).contains("Opener.get(").contains("Either.get"))
                .anySatisfy(message -> assertThat(message).contains("Either.getLeft"))
                .anySatisfy(message -> assertThat(message).contains("Either.isLeft"))
                .anySatisfy(message -> assertThat(message).contains("Either.isRight"))
                .anySatisfy(message -> assertThat(message).contains("Option.getOrElseThrow"))
                .anySatisfy(message -> assertThat(message).contains("Option.isEmpty"))
                .anySatisfy(message -> assertThat(message).contains("Try.getCause"))
                .anySatisfy(message -> assertThat(message).contains("Try.isFailure"))
                .anySatisfy(message -> assertThat(message).contains("Value.get"))
                .anySatisfy(message -> assertThat(message).contains("Either::get"));
        assertThat(violations).hasSize(11);
    }

    @Test
    void acceptsFoldMapAndDefaultsAndLeavesTheJdkAlone() {
        assertThat(rule.check(Fixtures.load("vavr.good"))).isEmpty();
    }
}
