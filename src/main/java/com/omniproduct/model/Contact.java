package com.omniproduct.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Embeddable
public class Contact {

    private String email;
    private String phone;

    public Contact() {}

    public Contact(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }

    @JsonProperty("contactEmail")
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @JsonProperty("contactPhone")
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
