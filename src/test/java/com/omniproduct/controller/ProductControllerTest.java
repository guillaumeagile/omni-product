package com.omniproduct.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniproduct.model.Product;
import com.omniproduct.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(ProductService.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCrudOperations() throws Exception {
        Product.Price price = new Product.Price(100.0, 20.0, 0.2);
        Product.Warehouse warehouse = new Product.Warehouse("Main Warehouse");
        Product product = new Product(
                "p1", "Test Product", "test-product",
                price, List.of("D1"), List.of("img1.jpg"),
                1.5, "10x10x10", 100, 50, warehouse
        );

        // Create
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.name").value("Test Product"));

        // Get All
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("p1"));

        // Get by ID
        mockMvc.perform(get("/api/products/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));

        // Update
        Product updatedProduct = new Product(
                "p1", "Updated Product", "updated-product",
                price, List.of("D1"), List.of("img1.jpg"),
                1.5, "10x10x10", 100, 50, warehouse
        );
        mockMvc.perform(put("/api/products/p1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"));

        // Delete
        mockMvc.perform(delete("/api/products/p1"))
                .andExpect(status().isNoContent());

        // Get by ID (Not Found)
        mockMvc.perform(get("/api/products/p1"))
                .andExpect(status().isNotFound());
    }
}
