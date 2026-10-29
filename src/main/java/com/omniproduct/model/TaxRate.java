package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record TaxRate(Double value) {
    public TaxRate {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("TaxRate cannot be null or negative");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TaxRate of(Double value) {
        return new TaxRate(value);
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
