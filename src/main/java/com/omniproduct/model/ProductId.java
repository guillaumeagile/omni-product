package com.omniproduct.model;

import jakarta.persistence.Embeddable;

@Embeddable
public record ProductId(String value) {
    public ProductId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ProductId cannot be null or blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
