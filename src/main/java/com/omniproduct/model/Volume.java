package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record Volume(String value) {
    public Volume {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Volume cannot be null or blank");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Volume of(String value) {
        return new Volume(value);
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
