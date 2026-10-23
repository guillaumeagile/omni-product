package com.omniproduct.harness.rules;

import com.omniproduct.harness.Fixtures;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMustImplementIdentifiableRuleTest {

    private final EntityMustImplementIdentifiableRule rule = new EntityMustImplementIdentifiableRule("..model..");

    @Test
    void flagsAModelClassThatIsNotIdentifiable() {
        List<Violation> violations = rule.check(Fixtures.load("ha9.bad"));

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).filePath()).isEqualTo("Orphan.java");
        assertThat(violations.get(0).line()).isEqualTo(4);
        assertThat(violations.get(0).message()).contains("Orphan", "does not implement <Identifiable>");
    }

    @Test
    void acceptsIdentifiableEntitiesAndRecordValueObjects() {
        assertThat(rule.check(Fixtures.load("ha9.good"))).isEmpty();
    }
}
