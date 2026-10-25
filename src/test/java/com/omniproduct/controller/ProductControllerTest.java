package com.omniproduct.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCrudOperations() throws Exception {
        // Create : le produit n'a que son identité et son nom
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id": "p1", "name": "Test Product"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.name").value("Test Product"));

        // Test the validation rule
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": ""}"""))
                .andExpect(status().isFailedDependency());

        // Get All
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("p1"));

        // Get by ID
        mockMvc.perform(get("/api/products/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));

        // Catalog : absent tant qu'on ne l'a pas écrit
        mockMvc.perform(get("/api/products/p1/catalog"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/products/p1/catalog")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"slug": "test-product", "discounts": ["D1"], "images": {"main": "image-url"}}"""))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products/p1/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-product"))
                .andExpect(jsonPath("$.images.main").value("image-url"));

        // Price
        mockMvc.perform(put("/api/products/p1/price")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"base": 100.0, "tax": 20.0, "taxRate": 0.2}"""))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products/p1/price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base").value(100.0));

        // Stock
        mockMvc.perform(put("/api/products/p1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"stock": 50, "quantity": 100, "warehouse": {"location": "Main Warehouse"}}"""))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products/p1/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(50))
                .andExpect(jsonPath("$.warehouse.location").value("Main Warehouse"));

        // Transport
        mockMvc.perform(put("/api/products/p1/transport")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"kilos": 23.0, "volume": "10x10x10"}"""))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products/p1/transport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volume").value("10x10x10"));

        // Sourcing
        mockMvc.perform(put("/api/products/p1/sourcing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"supplierId": "sup1", "suppliersRegions": {"Europe": {"name": "Supplier1", "siren": "SIREN123", "tvaId": "TVA456"}}}"""))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/products/p1/sourcing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierId").value("sup1"))
                .andExpect(jsonPath("$.suppliersRegions.Europe.siren").value("SIREN123"));

        // Une préoccupation d'un produit inconnu n'existe pas
        mockMvc.perform(put("/api/products/unknown/catalog")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"slug": "x"}"""))
                .andExpect(status().isNotFound());

        // Update : seul le nom change
        mockMvc.perform(put("/api/products/p1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Updated Product"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"));

        // Delete : les préoccupations partent avec le produit
        mockMvc.perform(delete("/api/products/p1"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/p1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products/p1/catalog"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products/p1/sourcing"))
                .andExpect(status().isNotFound());
    }
}
