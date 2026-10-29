package com.omniproduct.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class SupplierRegion {
    private String country;
    private String region;

    public SupplierRegion() {}

    public SupplierRegion(String country, String region) {
        this.country = country;
        this.region = region;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
