package com.omniproduct.model;

import jakarta.persistence.*;

@Embeddable
public class Contact {
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
