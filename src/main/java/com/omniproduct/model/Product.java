package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id;

    @Embedded
    @JsonUnwrapped
    private Catalog catalog = new Catalog();

    @Embedded
    private Price price;

    @Embedded
    @JsonUnwrapped
    private Storage storage = new Storage();

    @Embedded
    @JsonUnwrapped
    private Supply supply = new Supply();

    // Default constructor for JPA
    public Product() {}

    public Product(String id, Catalog catalog, Price price, Storage storage, Supply supply) {
        this.id = id;
        this.catalog = catalog;
        this.price = price;
        this.storage = storage;
        this.supply = supply;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Catalog getCatalog() { return catalog; }
    public void setCatalog(Catalog catalog) { this.catalog = catalog; }

    public Price getPrice() { return price; }
    public void setPrice(Price price) { this.price = price; }

    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }

    public Supply getSupply() { return supply; }
    public void setSupply(Supply supply) { this.supply = supply; }

    @Embeddable
    public static class Supplier {
        private String name;
        private String siren;
        private String tvaId;

        public Supplier() {}

        public Supplier(String name, String siren, String tvaId) {
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

    @Embeddable
    public static class Price {
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

    @Embeddable
    public static class Warehouse {
        private String location;

        public Warehouse() {}

        public Warehouse(String location) {
            this.location = location;
        }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }
}
