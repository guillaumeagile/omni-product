package com.omniproduct.model;

import jakarta.persistence.*;
import java.util.Map;

@Embeddable
public class Supply {

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ElementCollection
    @CollectionTable(name = "product_suppliers", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "region")
    private Map<String, Product.Supplier> suppliersRegions;

    public Supply() {}

    public Supply(Supplier supplier, Map<String, Product.Supplier> suppliersRegions) {
        this.supplier = supplier;
        this.suppliersRegions = suppliersRegions;
    }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public Map<String, Product.Supplier> getSuppliersRegions() { return suppliersRegions; }
    public void setSuppliersRegions(Map<String, Product.Supplier> suppliersRegions) { this.suppliersRegions = suppliersRegions; }
}
