package com.omniproduct.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ImpactCo2Request(
    @JsonProperty("transport_mode")
    String transportMode,
    
    @JsonProperty("distance_km")
    Double distanceKm,
    
    @JsonProperty("weight_kg")
    Double weightKg
) {}
