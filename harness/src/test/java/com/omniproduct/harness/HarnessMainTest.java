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

    // Dry run : le harnais DOIT trouver de vrais problèmes dans le code legacy de l'atelier.
    // S'il est vert ici, c'est lui qui est cassé, pas le code.
    @Test
    void failsOnTheLegacyWorkshopCode() {
        Run result = run("--phase", "1", "--classes", "../build/classes/java/main", "../src/main/java");

        assertThat(result.exitCode()).isEqualTo(1);
        // Chaque règle de la phase 1 qui doit trouver un problème dans le legacy le trouve.
        assertThat(result.out()).contains("[FAIL] HA5", "[FAIL] HA6", "[FAIL] HA7", "[FAIL] HA8", "[FAIL] HA9", "[FAIL] HA10");
        assertThat(result.out()).contains("'Product' has");
        assertThat(result.out()).contains("Supplier.java:", "which looks like a service");
        // HA4 (héritage) est une règle de la phase 2 : elle n'apparaît pas dans la sortie de la phase 1. HA2 non plus.
        assertThat(result.out()).doesNotContain("HA4", "HA2");
        assertThat(result.out()).contains("Product.java:");
        assertThat(result.out()).contains("HARNESS RESULT: FAIL");
    }

    // Le rapport JSON reflète la sortie texte : mêmes règles, dans l'ordre de la phase, mêmes statuts.
    @Test
    void writesAMachineReadableReport(@TempDir Path dir) {
        Path json = dir.resolve("report.json");

        Run result = run("--phase", "1", "--json", json.toString(), "--classes", "../build/classes/java/main", "../src/main/java");
        HarnessReport report = HarnessReport.read(json);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(report.phase()).isEqualTo("1");
        assertThat(report.result()).isEqualTo("FAIL");
        assertThat(report.rules()).extracting(HarnessReport.RuleResult::id)
                .isEqualTo(Phases.rules("1").stream().map(HarnessRule::id).toList());
        assertThat(report.rule("HA5")).hasValueSatisfying(rule -> {
            assertThat(rule.status()).isEqualTo("FAIL");
            assertThat(rule.violations()).isPositive();
            assertThat(result.out()).contains("[FAIL] HA5");
        });
        assertThat(report.rule("HA12.1")).hasValueSatisfying(rule -> {
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
