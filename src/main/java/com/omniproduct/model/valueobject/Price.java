package com.omniproduct.model.valueobject;

import jakarta.persistence.Embeddable;

@Embeddable
public record Price(Double base, Double tax, Double taxRate) {
}
