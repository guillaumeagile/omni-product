package com.omniproduct.model;

import java.util.List;
import java.util.Map;

public record Product(
    String id,
    String name,
    String slug,
    Price price,
    List<String> discounts,
    Map<String, ImageDetail> images,
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

    public record ImageDetail(
        String altText,
        Map<String, String> variants, // e.g., "full" -> "url1", "thumbnail" -> "url2"
        Integer width,
        Integer height,
        Double aspectRatio,
        Boolean transparent,
        Boolean watermarked
    ) {}
}
