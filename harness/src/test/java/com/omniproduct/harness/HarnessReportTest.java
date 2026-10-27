package com.omniproduct.harness;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HarnessReportTest {

    // Les JSON des checkouts d'équipe, écrits avant le renommage, gardent la clé « step » : l'arbitre doit les relire.
    @Test
    void readsTheStepKeyOfAReportWrittenBeforeTheRename(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("report.json");
        new HarnessReport("2", "PASS", List.<HarnessReport.RuleResult>of(), new Metrics(0, 0, 0, null, null)).write(file);
        Files.writeString(file, Files.readString(file).replace("\"phase\"", "\"step\""));

        assertThat(HarnessReport.read(file).phase()).isEqualTo("2");
    }
}
