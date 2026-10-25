package com.omniproduct.model;

// Product ne garde que son identité. Chaque préoccupation (catalogue, prix, stock, transport, approvisionnement)
// est une valeur à part, reliée au produit par son identifiant.
public class Product {

    private String id;
    private String name;

    public Product() {}

    public Product(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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

    public static class Warehouse {
        private String location;

        public Warehouse() {}

        public Warehouse(String location) {
            this.location = location;
        }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

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
}
