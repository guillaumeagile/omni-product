package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record Region(String value) {
    public Region {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Region cannot be null or blank");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Region of(String value) {
        return new Region(value);
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
