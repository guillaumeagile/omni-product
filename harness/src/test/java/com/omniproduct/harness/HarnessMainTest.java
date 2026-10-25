package com.omniproduct.harness;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

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

    // Dry run : le harnais DOIT trouver de vrais problèmes dans le code de départ de l'étape 2.
    // S'il est vert ici, c'est lui qui est cassé, pas le code.
    @Test
    void failsOnTheLegacyWorkshopCode() {
        Run result = run("--step", "1", "--classes", "../build/classes/java/main", "../src/main/java");

        assertThat(result.exitCode()).isEqualTo(1);
        // Le code de départ de l'étape 2 a fait l'étape 1, sauf HA7 : la hiérarchie Perishable/NonExpiring (exercice
        // HA4.1 de l'étape 2) référence l'entité Product. Le harnais la trouve.
        assertThat(result.out()).contains("[PASS] HA5", "[PASS] HA11", "[PASS] HA12 ", "[PASS] HA12.1", "[FAIL] HA7");
        assertThat(result.out()).contains("Perishable.java:", "NonExpiring.java:");
        // HA4 (héritage) est une règle de l'étape 2 : elle n'apparaît pas dans la sortie de l'étape 1. HA2 non plus.
        assertThat(result.out()).doesNotContain("HA4", "HA2");
        assertThat(result.out()).contains("HARNESS RESULT: FAIL");
    }

    // Le rapport JSON reflète la sortie texte : mêmes règles, dans l'ordre de l'étape, mêmes statuts.
    @Test
    void writesAMachineReadableReport(@TempDir Path dir) {
        Path json = dir.resolve("report.json");

        Run result = run("--step", "2", "--json", json.toString(), "--classes", "../build/classes/java/main", "../src/main/java");
        HarnessReport report = HarnessReport.read(json);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(report.step()).isEqualTo("2");
        assertThat(report.result()).isEqualTo("FAIL");
        assertThat(report.rules()).extracting(HarnessReport.RuleResult::id)
                .isEqualTo(Steps.rules("2").stream().map(HarnessRule::id).toList());
        // HA2 (immuabilité) est rouge sur le code de départ de l'étape 2 : Product et Supplier ont des champs mutables.
        assertThat(report.rule("HA2")).hasValueSatisfying(rule -> {
            assertThat(rule.status()).isEqualTo("FAIL");
            assertThat(rule.violations()).isPositive();
            assertThat(result.out()).contains("[FAIL] HA2");
        });
        // Exercice volontaire (Perishable.isPastSellBy lit LocalDate.now()) : HA4.6 est rouge sur le legacy de l'étape 2.
        assertThat(report.rule("HA4.6")).hasValueSatisfying(rule -> assertThat(rule.status()).isEqualTo("FAIL"));
        // Exercice volontaire (ProductService.sellOne retire du stock sans canSell) : l'avertissement HA4.7.
        assertThat(report.rule("HA4.7")).hasValueSatisfying(rule -> {
            assertThat(rule.status()).isEqualTo("WARN");
            assertThat(rule.severity()).isEqualTo("WARNING");
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
