package com.omniproduct.model;

import io.vavr.collection.Map;

public record ProductSourcing(SupplierId supplierId, Map<Region, Product.Supplier> suppliersRegions) {}
