package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

// Supplier garde son identité et son nom ; ses coordonnées et sa localisation sont des objets à part.
public class Supplier {
    private String id;

    private String name;

    private Contact contact;

    private Address address;

    public Supplier() {}

    public Supplier(String id, String name, String contactEmail, String contactPhone,
                   String country, String region) {
        this.id = id;
        this.name = name;
        this.contact = new Contact(contactEmail, contactPhone);
        this.address = new Address(country, region);
    }

    public static class Contact {
        private String contactEmail;
        private String contactPhone;

        public Contact() {}

        public Contact(String contactEmail, String contactPhone) {
            this.contactEmail = contactEmail;
            this.contactPhone = contactPhone;
        }

        public String getContactEmail() { return contactEmail; }
        public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    }

    public static class Address {
        private String country;
        private String region;

        public Address() {}

        public Address(String country, String region) {
            this.country = country;
            this.region = region;
        }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
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
    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}
