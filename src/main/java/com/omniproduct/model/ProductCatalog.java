package com.omniproduct.model;

import java.util.List;
import java.util.Map;

// Le catalogue : comment le produit se présente au client.
public record ProductCatalog(String slug, List<String> discounts, Map<String, String> images) {}
