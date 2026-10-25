package com.omniproduct.model;

import java.util.Map;

// L'approvisionnement : quel fournisseur, et les fournisseurs par région. Le fournisseur est référencé par son id.
public class ProductSourcing {

    private String supplierId;
    private Map<String, Product.Supplier> suppliersRegions;

    public ProductSourcing() {}

    public ProductSourcing(String supplierId, Map<String, Product.Supplier> suppliersRegions) {
        this.supplierId = supplierId;
        this.suppliersRegions = suppliersRegions;
    }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public Map<String, Product.Supplier> getSuppliersRegions() { return suppliersRegions; }
    public void setSuppliersRegions(Map<String, Product.Supplier> suppliersRegions) { this.suppliersRegions = suppliersRegions; }
}
