package com.omniproduct.domain.model;

public record TransportRequest(
    String transportMode,
    Double distanceKm,
    Double weightKg
) {
    public TransportRequest {
        if (transportMode == null || transportMode.isBlank()) {
            throw new IllegalArgumentException("Transport mode is required");
        }
        if (distanceKm == null || distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        if (weightKg == null || weightKg <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
    }
}
