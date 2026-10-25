package com.omniproduct.model;

import java.util.Map;

// L'approvisionnement : quel fournisseur, et les fournisseurs par région. Le fournisseur est référencé par son id.
public record ProductSourcing(String supplierId, Map<String, Product.Supplier> suppliersRegions) {}
