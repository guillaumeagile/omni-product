package com.omniproduct.harness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

// Le verdict du harnais lisible par une machine (`--json`) : ce que l'arbitre des PR compare entre la base et la tête.
// Déterministe : règles dans l'ordre de Phases, aucun horodatage. Les mesures (tokens, complexité) sont celles de model/ et service/.
public record HarnessReport(String phase, String result, List<RuleResult> rules, Metrics metrics) {

    public record RuleResult(String id, String name, String severity, String status, int violations) {
    }

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public Optional<RuleResult> rule(String id) {
        return rules.stream().filter(rule -> rule.id().equalsIgnoreCase(id)).findFirst();
    }

    public void write(Path file) {
        try {
            JSON.writeValue(file.toFile(), this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static HarnessReport read(Path file) {
        try {
            return JSON.readValue(file.toFile(), HarnessReport.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
