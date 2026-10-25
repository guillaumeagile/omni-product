package com.omniproduct.model;

import jakarta.persistence.*;

// Le transport : poids et volume du produit.
@Entity
@Table(name = "product_transports")
public class ProductTransport {

    @Id
    private String productId;

    private Double kilos;
    private String volume;

    public ProductTransport() {}

    public ProductTransport(String productId, Double kilos, String volume) {
        this.productId = productId;
        this.kilos = kilos;
        this.volume = volume;
    }

    public Double getKilos() { return kilos; }
    public void setKilos(Double kilos) { this.kilos = kilos; }

    public String getVolume() { return volume; }
    public void setVolume(String volume) { this.volume = volume; }
}
