package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record Quantity(Integer value) {
    public Quantity {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("Quantity cannot be null or negative");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Quantity of(Integer value) {
        return new Quantity(value);
    }

    @JsonValue
    public Integer value() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
