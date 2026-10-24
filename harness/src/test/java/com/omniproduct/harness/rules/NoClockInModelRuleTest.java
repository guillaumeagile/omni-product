package com.omniproduct.harness.rules;

import com.omniproduct.harness.Fixtures;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoClockInModelRuleTest {

    private final NoClockInModelRule rule = new NoClockInModelRule("..model..");

    @Test
    void flagsEveryWayOfReadingTheClock() {
        List<Violation> violations = rule.check(Fixtures.load("clock.bad"));

        assertThat(violations).extracting(Violation::message)
                .anySatisfy(message -> assertThat(message).contains("LocalDate.now"))
                .anySatisfy(message -> assertThat(message).contains("Instant.now"))
                .anySatisfy(message -> assertThat(message).contains("System.currentTimeMillis"))
                .anySatisfy(message -> assertThat(message).contains("java.util.Date.<init>"))
                .anySatisfy(message -> assertThat(message).contains("Clock.systemUTC"));
    }

    @Test
    void acceptsADateGivenAsAParameter() {
        assertThat(rule.check(Fixtures.load("clock.good"))).isEmpty();
    }
}
