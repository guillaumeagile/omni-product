package com.omniproduct.model;

// Le transport : poids et volume du produit.
public class ProductTransport {

    private Double kilos;
    private String volume;

    public ProductTransport() {}

    public ProductTransport(Double kilos, String volume) {
        this.kilos = kilos;
        this.volume = volume;
    }

    public Double getKilos() { return kilos; }
    public void setKilos(Double kilos) { this.kilos = kilos; }

    public String getVolume() { return volume; }
    public void setVolume(String volume) { this.volume = volume; }
}
