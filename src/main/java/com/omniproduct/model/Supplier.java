package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.omniproduct.service.ProductService;
import com.omniproduct.service.SupplierService;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    private String id;

    private String name;

    @Embedded
    private Contact contact;

    @Embedded
    private Location location;

    @OneToMany(mappedBy = "supply.supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products;

    public Supplier() {}

    public Supplier(String id, String name, String contactEmail, String contactPhone,
                   String country, String region) {
        this.id = id;
        this.name = name;
        this.contact = new Contact(contactEmail, contactPhone);
        this.location = new Location(country, region);
    }

    public Supplier(String id, String name, ProductService productService, SupplierService supplierService) {
        this.id = id;
        this.name = name;
        this.products = productService.findAll();
        this.location = new Location(null, supplierService.getSupplier(id).map(Supplier::getLocation).map(Location::getRegion).orElse(null));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonUnwrapped
    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    @JsonUnwrapped
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    @Embeddable
    public static class Contact {
        private String contactEmail;
        private String contactPhone;

        public Contact() {}

        public Contact(String contactEmail, String contactPhone) {
            this.contactEmail = contactEmail;
            this.contactPhone = contactPhone;
        }

        public String getContactEmail() {
            return contactEmail;
        }

        public void setContactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public void setContactPhone(String contactPhone) {
            this.contactPhone = contactPhone;
        }
    }

    @Embeddable
    public static class Location {
        private String country;
        private String region;

        public Location() {}

        public Location(String country, String region) {
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
}
