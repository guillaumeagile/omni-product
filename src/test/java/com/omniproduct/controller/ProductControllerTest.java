package com.omniproduct.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniproduct.model.Product;
import com.omniproduct.model.BasePrice;
import com.omniproduct.model.TaxAmount;
import com.omniproduct.model.TaxRate;
import com.omniproduct.model.Discount;
import com.omniproduct.model.ImageDetail;
import com.omniproduct.model.ImageKey;
import com.omniproduct.model.Kilos;
import com.omniproduct.model.Location;
import com.omniproduct.model.Quantity;
import com.omniproduct.model.Region;
import com.omniproduct.model.Slug;
import com.omniproduct.model.Stock;
import com.omniproduct.model.Volume;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCrudOperations() throws Exception {
        Product.Price price = new Product.Price(new BasePrice(100.0), new TaxAmount(20.0), new TaxRate(0.2));
        Product.Warehouse warehouse = new Product.Warehouse(new Location("Main Warehouse"));
        Product.Supplier supplier1 = new Product.Supplier("Supplier1", "SIREN123", "TVA456");
        Product product = new Product(
                "p1", "Test Product", new Slug("test-product"),
                price, List.of(new Discount("Image 1")), Map.of(new ImageKey("main"), new ImageDetail("image-url")),
                Map.of(new Region("Europe"), supplier1),
                new Kilos(23.0), new Volume("10x10x10"), new Quantity(100), new Stock(50), warehouse
        );

        // Create
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.images.main").value("image-url"))
                .andExpect(jsonPath("$.suppliersRegions.Europe.name").value("Supplier1"))
                .andExpect(jsonPath("$.suppliersRegions.Europe.siren").value("SIREN123"))
                .andExpect(jsonPath("$.suppliersRegions.Europe.tvaId").value("TVA456"));

        // Test the  validation rule
        Product invalidProduct = new Product();
        invalidProduct.setName(""); // Trigger the exception

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProduct)))
                .andExpect(status().isFailedDependency());

        // Get All
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("p1"));

        // Get by ID
        mockMvc.perform(get("/api/products/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));

        // Update
        Product.Supplier supplier2 = new Product.Supplier("Supplier2", "SIREN789", "TVA012");
        Product updatedProduct = new Product(
                "p1", "Updated Product", new Slug("updated-product"),
                price, List.of(new Discount("D1")), Map.of(new ImageKey("main"), new ImageDetail("image-url")),
                Map.of(new Region("Asia"), supplier2),
                new Kilos(3.0), new Volume("10x10x10"), new Quantity(100), new Stock(50), warehouse
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
