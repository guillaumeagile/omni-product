package com.omniproduct.harness.rules;

import com.omniproduct.harness.Fixtures;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Passe faux négatifs des règles bytecode (ArchUnit) : chaque fixture contient un contournement plausible.
class ArchEvasionTest {

    @Test
    void ha4SeesAnAnonymousSubclass() {
        List<Violation> violations = new NoInheritanceRule("..model..").check(Fixtures.load("ha4.evasion"));
        assertThat(violations).anyMatch(v -> v.message().contains("Base"));
    }

    @Test
    void ha7SeesAnEntityReferencedThroughAGenericReturnType() {
        List<Violation> violations = new FanOutRule("..model..").check(Fixtures.load("ha7.evasion"));
        assertThat(violations).anyMatch(v -> v.message().contains("Product") && v.message().contains("Supplier"));
    }

    @Test
    void ha7SeesAnEntityLeakingThroughAValueObject() {
        List<Violation> violations = new FanOutRule("..model..").check(Fixtures.load("ha7.evasion"));
        assertThat(violations).anyMatch(v -> v.message().contains("Basket") && v.message().contains("Offer"));
    }

    @Test
    void ha9SeesARecordThatCarriesAnIdentity() {
        List<Violation> violations = new EntityMustImplementIdentifiableRule("..model..").check(Fixtures.load("ha9.evasion"));
        assertThat(violations).anyMatch(v -> v.message().contains("Line"));
    }

    @Test
    void ha9AcceptsIdentifiableInheritedThroughAnInterface() {
        List<Violation> violations = new EntityMustImplementIdentifiableRule("..model..").check(Fixtures.load("ha9.evasion"));
        assertThat(violations).noneMatch(v -> v.message().contains("Order"));
        assertThat(violations).noneMatch(v -> v.message().contains("Tag"));
    }
}
