package com.omniproduct.model;

import java.util.List;
import java.util.Map;

public record Catalogue(String name, String slug, List<String> discounts, Map<String, String> images) {}
