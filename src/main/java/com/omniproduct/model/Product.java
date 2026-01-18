package com.omniproduct.model;

import java.util.List;

public record Product(
    String id,
    String name,
    String slug,
    Price price,
    List<String> discounts,
    List<String> images,
    Double weight,
    String dimensions,
    Integer quantity,
    Integer stock,
    Warehouse warehouse
) {
    public record Price(
        Double base,
        Double tax,
        Double taxRate
    ) {}

    public record Warehouse(
        String location
    ) {}
}
