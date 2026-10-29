package com.omniproduct.model;

import jakarta.persistence.*;

@Embeddable
public class Price {
    private Double base;
    private Double tax;
    private Double taxRate;
    
    public Price() {}
    
    public Price(Double base, Double tax, Double taxRate) {
        this.base = base;
        this.tax = tax;
        this.taxRate = taxRate;
    }
    
    public Double getBase() { return base; }
    public void setBase(Double base) { this.base = base; }
    
    public Double getTax() { return tax; }
    public void setTax(Double tax) { this.tax = tax; }
    
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
}
