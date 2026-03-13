package com.omniproduct.domain.model;

public record TransportCarbonMetrics(
    Double co2EquivalentKg,
    Double co2DirectKg,
    String transportMode,
    Double distance,
    Double weight
) {
    public TransportCarbonMetrics {
        if (co2EquivalentKg < 0 || distance < 0 || weight < 0) {
            throw new IllegalArgumentException("Carbon metrics cannot be negative");
        }
    }
}
