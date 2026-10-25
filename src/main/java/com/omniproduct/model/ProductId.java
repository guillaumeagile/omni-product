package com.omniproduct.model;

import java.util.UUID;

// Identifiant généré par le système : un record, pour pouvoir passer de l'UUID v7 au ULID sans toucher au modèle.
public record ProductId(UUID value) {}
