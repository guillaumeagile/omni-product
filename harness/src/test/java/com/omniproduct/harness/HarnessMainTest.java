package com.omniproduct.harness;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HarnessMainTest {

    private record Run(int exitCode, String out) {
    }

    private static Run run(String... args) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true));
        try {
            return new Run(HarnessMain.run(args), buffer.toString());
        } finally {
            System.setOut(original);
        }
    }

    // Le code de départ de la phase 3 a fait les phases 1 et 2 : leurs étapes sont vertes.
    @Test
    void theEarlierStepsAreGreenOnThePhase3StartCode() {
        Run step1 = run("--step", "1", "--classes", "../build/classes/java/main", "../src/main/java");
        Run step2 = run("--step", "2", "--classes", "../build/classes/java/main", "../src/main/java");

        assertThat(step1.exitCode()).isZero();
        assertThat(step2.exitCode()).isZero();
        assertThat(step2.out()).contains("[PASS] HA2 ", "[PASS] HA4.1", "[PASS] HA4.5", "[PASS] HA4.7", "HARNESS RESULT: PASS");
        // HA14 et HA15 sont des règles de l'étape 3 : l'étape 2 ne les connaît pas.
        assertThat(step2.out()).doesNotContain("HA14", "HA15");
    }

    // Dry run : le harnais DOIT trouver de vrais problèmes dans le code de départ de la phase 3, et eux seuls
    // (l'exercice : une boucle qui remplit une liste, une fabrique qui branche). S'il est vert ici, c'est lui qui est
    // cassé, pas le code.
    @Test
    void failsOnTheStartCodeOfPhase3ExactlyOnTheFunctionalStyleRules() {
        Run result = run("--step", "3", "--classes", "../build/classes/java/main", "../src/main/java");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.out().lines().filter(line -> line.startsWith("[FAIL]")).map(line -> line.split(" ")[1]).toList())
                .containsExactly("HA14", "HA15", "HA16", "HA17", "HA18");
        assertThat(result.out()).contains("CatalogService.java:", "Perishable.java:", "ProductService.java:", "Product.java:", "Supplier.java:");
        assertThat(result.out()).contains("HARNESS RESULT: FAIL");
    }

    // Le rapport JSON reflète la sortie texte : mêmes règles, dans l'ordre de l'étape, mêmes statuts.
    // L'étape 3 ajoute HA14, HA15, HA16, HA17, HA18 et HA21 à l'étape 2 ; `all` est l'étape 3. La numérotation saute le 13.
    @Test
    void writesAMachineReadableReport(@TempDir Path dir) {
        Path json = dir.resolve("report.json");

        Run result = run("--step", "3", "--json", json.toString(), "--classes", "../build/classes/java/main", "../src/main/java");
        HarnessReport report = HarnessReport.read(json);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(report.step()).isEqualTo("3");
        assertThat(report.result()).isEqualTo("FAIL");
        assertThat(report.rules()).extracting(HarnessReport.RuleResult::id)
                .isEqualTo(Steps.rules("3").stream().map(HarnessRule::id).toList())
                .endsWith("HA14", "HA15", "HA16", "HA17", "HA18", "HA21")
                .startsWith(Steps.rules("2").stream().map(HarnessRule::id).toArray(String[]::new));
        assertThat(Steps.rules("all").stream().map(HarnessRule::id).toList())
                .isEqualTo(Steps.rules("3").stream().map(HarnessRule::id).toList());
        assertThat(Steps.rules("3").stream().map(HarnessRule::id)).doesNotContain("HA13");
        assertThat(report.rules().stream().filter(rule -> rule.status().equals("FAIL")).map(HarnessReport.RuleResult::id).toList())
                .isEqualTo(List.of("HA14", "HA15", "HA16", "HA17", "HA18"));
        // Exercice volontaire (CatalogService.catalogLabels remplit une liste dans une boucle) : HA14 est rouge.
        assertThat(report.rule("HA14")).hasValueSatisfying(rule -> {
            assertThat(rule.status()).isEqualTo("FAIL");
            assertThat(rule.violations()).isPositive();
        });
        // Exercice volontaire (Perishable.of branche avec un `if`) : HA15 est rouge.
        assertThat(report.rule("HA15")).hasValueSatisfying(rule -> {
            assertThat(rule.status()).isEqualTo("FAIL");
            assertThat(rule.severity()).isEqualTo("ERROR");
            assertThat(rule.violations()).isPositive();
        });
        // Exercice volontaire (la garde de ProductService.sellOne teste canSellOn avec un `if`) : HA16 est rouge.
        assertThat(report.rule("HA16")).hasValueSatisfying(rule -> {
            assertThat(rule.status()).isEqualTo("FAIL");
            assertThat(rule.severity()).isEqualTo("ERROR");
            assertThat(rule.violations()).isEqualTo(1);
        });
        // Exercice volontaire (ProductService.withdrawStock reconstruit un ProductStock champ par champ) : HA17 est rouge.
        assertThat(report.rule("HA17")).hasValueSatisfying(rule -> {
            assertThat(rule.status()).isEqualTo("FAIL");
            assertThat(rule.severity()).isEqualTo("ERROR");
            assertThat(rule.violations()).isEqualTo(1);
        });
        // Exercice volontaire (Product.Warehouse, Product.Supplier, Supplier.Contact, Supplier.Address) : HA18 est rouge.
        assertThat(report.rule("HA18")).hasValueSatisfying(rule -> {
            assertThat(rule.status()).isEqualTo("FAIL");
            assertThat(rule.severity()).isEqualTo("ERROR");
            assertThat(rule.violations()).isEqualTo(4);
        });
    }

    @Test
    void refusesToPassWhenNothingIsCompiled(@TempDir Path empty) throws Exception {
        Path sources = Files.createDirectory(empty.resolve("src"));
        Files.writeString(sources.resolve("A.java"), "package a.model; public record A() {}");

        Run result = run("--classes", empty.resolve("classes").toString(), sources.toString());

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.out()).doesNotContain("PASS");
    }

    @Test
    void usageErrorWithoutArguments() {
        assertThat(run().exitCode()).isEqualTo(2);
    }
}
