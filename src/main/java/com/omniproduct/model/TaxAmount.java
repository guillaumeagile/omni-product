package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record TaxAmount(Double value) {
    public TaxAmount {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("TaxAmount cannot be null or negative");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TaxAmount of(Double value) {
        return new TaxAmount(value);
    }

    @JsonValue
    public Double value() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
