package com.omniproduct.harness.fixtures.bad.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// Volontairement mauvais : le domaine connaît JPA (HA8).
@Entity
public class PersistentThing {
    @Id
    private String id;
}
