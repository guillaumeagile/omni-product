package com.omniproduct.harness;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HarnessReportTest {

    // Le JSON reste « step » sur le fil : les checkouts d'équipe et la base lisent cette clé. Le nom Java est phase.
    @Test
    void writesTheStepKeyAndReadsItBack(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("report.json");
        new HarnessReport("2", "PASS", List.<HarnessReport.RuleResult>of(), new Metrics(0, 0, 0, null, null)).write(file);

        assertThat(Files.readString(file)).contains("\"step\" : \"2\"");
        assertThat(HarnessReport.read(file).phase()).isEqualTo("2");
    }
}
