package com.omniproduct.harness.rules;

import com.omniproduct.harness.Fixtures;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoInheritanceRuleTest {

    private final NoInheritanceRule rule = new NoInheritanceRule("..model..", "..service..");

    @Test
    void flagsClassesDerivingFromAnotherClassInModelAndService() {
        List<Violation> violations = rule.check(Fixtures.load("ha4.bad"));

        assertThat(violations).hasSize(2);
        // La ligne vient de l'AST : la déclaration de la classe dans le source, pas ":0".
        assertThat(violations).anySatisfy(v -> {
            assertThat(v.filePath()).isEqualTo("Derived.java");
            assertThat(v.line()).isEqualTo(4);
            assertThat(v.message()).contains("Derived", "extends <com.omniproduct.harness.fixtures.ha4.bad.model.Base>");
        });
        assertThat(violations).anySatisfy(v -> {
            assertThat(v.filePath()).isEqualTo("ChildService.java");
            assertThat(v.line()).isEqualTo(4);
        });
    }

    @Test
    void acceptsInterfacesRecordsEnumsAndExceptions() {
        assertThat(rule.check(Fixtures.load("ha4.good"))).isEmpty();
    }
}
