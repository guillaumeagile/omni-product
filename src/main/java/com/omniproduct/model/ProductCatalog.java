package com.omniproduct.model;

import io.vavr.collection.List;

public record ProductCatalog(Slug slug, List<Discount> discounts, List<Image> images) {}
