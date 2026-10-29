package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record Slug(String value) {
    public Slug {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Slug cannot be null or blank");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Slug of(String value) {
        return new Slug(value);
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
