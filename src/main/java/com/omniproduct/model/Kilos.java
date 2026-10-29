package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record Kilos(Double value) {
    public Kilos {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("Kilos cannot be null or negative");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Kilos of(Double value) {
        return new Kilos(value);
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
