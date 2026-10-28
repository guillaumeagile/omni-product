package com.omniproduct.model;

import jakarta.persistence.*;
import java.util.Map;

@Embeddable
public class Supply {

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "supplier_id"))
    private SupplierId supplierId;

    @ElementCollection
    @CollectionTable(name = "product_suppliers", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "region")
    private Map<String, Product.Supplier> suppliersRegions;

    public Supply() {}

    public Supply(SupplierId supplierId, Map<String, Product.Supplier> suppliersRegions) {
        this.supplierId = supplierId;
        this.suppliersRegions = suppliersRegions;
    }

    public SupplierId getSupplierId() { return supplierId; }
    public void setSupplierId(SupplierId supplierId) { this.supplierId = supplierId; }

    public Map<String, Product.Supplier> getSuppliersRegions() { return suppliersRegions; }
    public void setSuppliersRegions(Map<String, Product.Supplier> suppliersRegions) { this.suppliersRegions = suppliersRegions; }
}
