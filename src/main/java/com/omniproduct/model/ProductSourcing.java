package com.omniproduct.model;

import jakarta.persistence.*;
import java.util.Map;

// L'approvisionnement : quel fournisseur, et les fournisseurs par région. Le fournisseur est référencé par son id.
@Entity
@Table(name = "product_sourcings")
public class ProductSourcing {

    @Id
    private String productId;

    private String supplierId;

    @ElementCollection
    @CollectionTable(name = "product_suppliers", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "region")
    private Map<String, Product.Supplier> suppliersRegions;

    public ProductSourcing() {}

    public ProductSourcing(String productId, String supplierId, Map<String, Product.Supplier> suppliersRegions) {
        this.productId = productId;
        this.supplierId = supplierId;
        this.suppliersRegions = suppliersRegions;
    }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public Map<String, Product.Supplier> getSuppliersRegions() { return suppliersRegions; }
    public void setSuppliersRegions(Map<String, Product.Supplier> suppliersRegions) { this.suppliersRegions = suppliersRegions; }
}
