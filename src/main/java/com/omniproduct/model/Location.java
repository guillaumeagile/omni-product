package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record Location(String value) {
    public Location {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Location cannot be null or blank");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Location of(String value) {
        return new Location(value);
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
