package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record BasePrice(Double value) {
    public BasePrice {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("BasePrice cannot be null or negative");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BasePrice of(Double value) {
        return new BasePrice(value);
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
