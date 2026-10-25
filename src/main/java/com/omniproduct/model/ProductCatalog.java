package com.omniproduct.model;

import java.util.List;
import java.util.Map;

public record ProductCatalog(String slug, List<String> discounts, Map<String, String> images) {}
