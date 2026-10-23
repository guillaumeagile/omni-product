package com.omniproduct.harness.rules;

import com.omniproduct.harness.Fixtures;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoServicesInEntityConstructorRuleTest {

    private final NoServicesInEntityConstructorRule rule = new NoServicesInEntityConstructorRule("..model..");

    @Test
    void flagsOnlyTheConstructorTakingAService() {
        List<Violation> violations = rule.check(Fixtures.load("ha10.bad"));

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).filePath()).isEqualTo("Invoice.java");
        assertThat(violations.get(0).line()).isEqualTo(11);
        assertThat(violations.get(0).message()).contains("PricingService", "looks like a service");
    }

    @Test
    void acceptsConstructorsBuiltFromValues() {
        assertThat(rule.check(Fixtures.load("ha10.good"))).isEmpty();
    }
}
