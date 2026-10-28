package com.omniproduct.model;

import jakarta.persistence.*;

@Embeddable
public class Parcel {

    private Double kilos;
    private String volume;

    public Parcel() {}

    public Parcel(Double kilos, String volume) {
        this.kilos = kilos;
        this.volume = volume;
    }

    public Double getKilos() { return kilos; }
    public void setKilos(Double kilos) { this.kilos = kilos; }

    public String getVolume() { return volume; }
    public void setVolume(String volume) { this.volume = volume; }
}
