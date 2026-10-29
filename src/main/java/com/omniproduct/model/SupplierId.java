package com.omniproduct.model;

import jakarta.persistence.Embeddable;

@Embeddable
public record SupplierId(String value) {
    public SupplierId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SupplierId cannot be null or blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
