package com.omniproduct.model;

import jakarta.persistence.*;

@Embeddable
public class SupplierIdentity {
    private String name;
    private String siren;
    private String tvaId;
    
    public SupplierIdentity() {}
    
    public SupplierIdentity(String name, String siren, String tvaId) {
        this.name = name;
        this.siren = siren;
        this.tvaId = tvaId;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSiren() { return siren; }
    public void setSiren(String siren) { this.siren = siren; }

    public String getTvaId() { return tvaId; }
    public void setTvaId(String tvaId) { this.tvaId = tvaId; }
}
