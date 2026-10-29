package com.omniproduct.model;

import java.util.Map;

public record Sourcing(Supplier supplier, Map<String, Product.Supplier> suppliersRegions, Double kilos, String volume) {}
