package com.omniproduct.model;

import java.util.Map;

public record ProductSourcing(SupplierId supplierId, Map<Region, Product.Supplier> suppliersRegions) {}
