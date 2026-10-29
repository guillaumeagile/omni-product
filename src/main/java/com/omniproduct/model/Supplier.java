package com.omniproduct.model;

import jakarta.persistence.*;

@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    private String id;

    private String name;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "contact_email"))
    private ContactEmail contactEmail;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "contact_phone"))
    private ContactPhone contactPhone;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "country"))
    private Country country;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "region"))
    private Region region;

    public Supplier() {}

    public Supplier(String id, String name, ContactEmail contactEmail, ContactPhone contactPhone,
                   Country country, Region region) {
        this.id = id;
        this.name = name;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.country = country;
        this.region = region;
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

    public ContactEmail getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(ContactEmail contactEmail) {
        this.contactEmail = contactEmail;
    }

    public ContactPhone getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(ContactPhone contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }
}
