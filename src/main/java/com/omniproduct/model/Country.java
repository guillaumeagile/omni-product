package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record Country(String value) {
    public Country {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Country cannot be null or blank");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Country of(String value) {
        return new Country(value);
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
