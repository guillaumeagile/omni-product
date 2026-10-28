package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;

@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    private String id;

    private String name;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "email", column = @Column(name = "contactEmail")),
        @AttributeOverride(name = "phone", column = @Column(name = "contactPhone"))
    })
    private Contact contact = new Contact();

    @Embedded
    private Location location = new Location();

    public Supplier() {}

    public Supplier(String id, String name, String contactEmail, String contactPhone,
                   String country, String region) {
        this.id = id;
        this.name = name;
        this.contact = new Contact(contactEmail, contactPhone);
        this.location = new Location(country, region);
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
}
