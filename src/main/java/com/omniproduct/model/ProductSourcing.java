package com.omniproduct.model;

import java.util.Map;

public record ProductSourcing(String supplierId, Map<String, Product.Supplier> suppliersRegions) {}
