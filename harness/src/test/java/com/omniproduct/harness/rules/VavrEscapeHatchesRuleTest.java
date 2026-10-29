package com.omniproduct.harness.rules;

import com.omniproduct.harness.Fixtures;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VavrEscapeHatchesRuleTest {

    private final VavrEscapeHatchesRule rule = new VavrEscapeHatchesRule("..model..");

    @Test
    void flagsEveryWayOutOfAVavrContainerButFold() {
        List<Violation> violations = rule.check(Fixtures.load("vavr.bad"));

        assertThat(violations).extracting(Violation::message)
                .anySatisfy(message -> assertThat(message).contains("Opener.get(").contains("Either.get>"))
                .anySatisfy(message -> assertThat(message).contains("Either.getLeft"))
                .anySatisfy(message -> assertThat(message).contains("Either.isLeft"))
                .anySatisfy(message -> assertThat(message).contains("Either.isRight"))
                .anySatisfy(message -> assertThat(message).contains("Option.getOrElseThrow"))
                .anySatisfy(message -> assertThat(message).contains("Option.isEmpty"))
                .anySatisfy(message -> assertThat(message).contains("Try.getCause"))
                .anySatisfy(message -> assertThat(message).contains("Try.isFailure"))
                .anySatisfy(message -> assertThat(message).contains("Value.get"))
                .anySatisfy(message -> assertThat(message).contains("Either::get"))
                // Hors de toute liste de mots : la sortie se lit au type de retour.
                .anySatisfy(message -> assertThat(message).contains("Option.getOrNull"))
                .anySatisfy(message -> assertThat(message).contains("Option.getOrElse>"))
                .anySatisfy(message -> assertThat(message).contains("Try.getOrElseGet"))
                .anySatisfy(message -> assertThat(message).contains("Option.toJavaOptional"))
                .anySatisfy(message -> assertThat(message).contains("Option.toList"))
                .anySatisfy(message -> assertThat(message).contains("Option.contains"))
                .anySatisfy(message -> assertThat(message).contains("Option.forEach"))
                // Le fold d'insideFold ne blanchit pas le get() de son lambda.
                .anySatisfy(message -> assertThat(message).contains("Opener.insideFold(").contains("Option.get>"));
        assertThat(violations).extracting(Violation::message).noneMatch(message -> message.contains("<Either.fold>"));
        // 19 sorties, et pas d'absence : le fold d'insideFold existe, seul son lambda fuit.
        assertThat(violations).hasSize(19);
    }

    @Test
    void reportsTheLineOfTheCallNotOfTheMethod() {
        assertThat(rule.check(Fixtures.load("vavr.bad")))
                .filteredOn(violation -> violation.message().contains("Option.getOrNull"))
                .singleElement()
                .satisfies(violation -> assertThat(violation.filePath()).isEqualTo("Opener.java"))
                .satisfies(violation -> assertThat(violation.line()).isEqualTo(58));
    }

    @Test
    void acceptsFoldTransformationsReturnedContainersAndTheJdk() {
        assertThat(rule.check(Fixtures.load("vavr.good"))).isEmpty();
    }

    @Test
    void isRedByAbsenceWhenNoContainerIsFolded() {
        assertThat(rule.check(Fixtures.load("vavr.unfolded")))
                .singleElement()
                .satisfies(violation -> assertThat(violation.filePath()).isEqualTo("Returner.java"))
                .satisfies(violation -> assertThat(violation.message()).contains("consumed by fold"));
    }
}
