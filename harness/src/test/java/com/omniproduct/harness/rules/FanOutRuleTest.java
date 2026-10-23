package com.omniproduct.harness.rules;

import com.omniproduct.harness.Fixtures;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FanOutRuleTest {

    private final FanOutRule rule = new FanOutRule("..model..");

    @Test
    void flagsEntityReferencesInFieldsGenericsAndParameters() {
        List<Violation> violations = rule.check(Fixtures.load("ha7.bad"));

        assertThat(violations).allMatch(v -> v.filePath().equals("Order.java"));
        assertThat(violations).extracting(Violation::line).contains(8, 9, 11);
        assertThat(violations).anyMatch(v -> v.message().contains("Order.watchers"));
        assertThat(violations).anyMatch(v -> v.message().contains("assign("));
    }

    @Test
    void acceptsReferencesByIdAndValueObjects() {
        assertThat(rule.check(Fixtures.load("ha7.good"))).isEmpty();
    }
}
