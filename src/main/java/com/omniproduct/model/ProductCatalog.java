package com.omniproduct.model;

import java.util.List;

public record ProductCatalog(Slug slug, List<Discount> discounts, List<Image> images) {}
