package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;

@Embeddable
public record Stock(Integer value) {
    public Stock {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("Stock cannot be null or negative");
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Stock of(Integer value) {
        return new Stock(value);
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
