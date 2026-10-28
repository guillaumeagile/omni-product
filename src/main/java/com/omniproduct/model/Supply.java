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
    private Map<String, SupplierIdentity> suppliersRegions;
    
    private Double kilos;
    private String volume;
    
    public Supply() {}
    
    public Supply(Supplier supplier, Map<String, SupplierIdentity> suppliersRegions, Double kilos, String volume) {
        this.supplier = supplier;
        this.suppliersRegions = suppliersRegions;
        this.kilos = kilos;
        this.volume = volume;
    }
    
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    
    public Map<String, SupplierIdentity> getSuppliersRegions() { return suppliersRegions; }
    public void setSuppliersRegions(Map<String, SupplierIdentity> suppliersRegions) { this.suppliersRegions = suppliersRegions; }
    
    public Double getKilos() { return kilos; }
    public void setKilos(Double kilos) { this.kilos = kilos; }
    
    public String getVolume() { return volume; }
    public void setVolume(String volume) { this.volume = volume; }
}
