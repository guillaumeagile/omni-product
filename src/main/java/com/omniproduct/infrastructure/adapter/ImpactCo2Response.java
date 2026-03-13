package com.omniproduct.infrastructure.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ImpactCo2Response(
    @JsonProperty("co2_equivalent_kg")
    Double co2Equivalent,
    
    @JsonProperty("co2_direct_kg")
    Double co2Direct,
    
    @JsonProperty("transport_mode")
    String transportMode,
    
    @JsonProperty("distance_km")
    Double distance,
    
    @JsonProperty("weight_kg")
    Double weight
) {}
