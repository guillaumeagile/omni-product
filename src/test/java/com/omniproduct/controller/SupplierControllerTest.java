package com.omniproduct.controller;

import com.omniproduct.model.Supplier;
import com.omniproduct.model.Product;
import com.omniproduct.repository.SupplierRepository;
import com.omniproduct.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SupplierControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        supplierRepository.deleteAll();
        productRepository.deleteAll();
    }
    
    @Test
    void shouldCreateSupplier() throws Exception {
        String supplierJson = """
            {
                "id": "sup1",
                "name": "Supplier A",
                "contactEmail": "contact@suppliera.com",
                "contactPhone": "+33123456789",
                "country": "France",
                "region": "Île-de-France"
            }
            """;
        
        mockMvc.perform(post("/api/suppliers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(supplierJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("sup1"))
                .andExpect(jsonPath("$.name").value("Supplier A"))
                .andExpect(jsonPath("$.country").value("France"));
    }
    
    @Test
    void shouldGetSupplierById() throws Exception {
        Supplier supplier = new Supplier("sup1", "Supplier A", "contact@suppliera.com", 
                                        "+33123456789", "France", "Île-de-France");
        supplierRepository.save(supplier);
        
        mockMvc.perform(get("/api/suppliers/sup1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("sup1"))
                .andExpect(jsonPath("$.name").value("Supplier A"));
    }
    
    @Test
    void shouldReturnNotFoundForNonExistentSupplier() throws Exception {
        mockMvc.perform(get("/api/suppliers/nonexistent"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldGetAllSuppliers() throws Exception {
        Supplier supplier1 = new Supplier("sup1", "Supplier A", "contact@suppliera.com", 
                                         "+33123456789", "France", "Île-de-France");
        Supplier supplier2 = new Supplier("sup2", "Supplier B", "contact@supplierb.com", 
                                         "+33987654321", "Spain", "Madrid");
        supplierRepository.save(supplier1);
        supplierRepository.save(supplier2);
        
        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Supplier A"))
                .andExpect(jsonPath("$[1].name").value("Supplier B"));
    }
    
    @Test
    void shouldGetSuppliersByCountry() throws Exception {
        Supplier supplier1 = new Supplier("sup1", "Supplier A", "contact@suppliera.com", 
                                         "+33123456789", "France", "Île-de-France");
        Supplier supplier2 = new Supplier("sup2", "Supplier B", "contact@supplierb.com", 
                                         "+33987654321", "Spain", "Madrid");
        supplierRepository.save(supplier1);
        supplierRepository.save(supplier2);
        
        mockMvc.perform(get("/api/suppliers/country/France"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Supplier A"));
    }
    
    @Test
    void shouldGetSuppliersByRegion() throws Exception {
        Supplier supplier1 = new Supplier("sup1", "Supplier A", "contact@suppliera.com", 
                                         "+33123456789", "France", "Île-de-France");
        Supplier supplier2 = new Supplier("sup2", "Supplier B", "contact@supplierb.com", 
                                         "+33987654321", "France", "Provence");
        supplierRepository.save(supplier1);
        supplierRepository.save(supplier2);
        
        mockMvc.perform(get("/api/suppliers/region/Île-de-France"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Supplier A"));
    }
    
    @Test
    void shouldUpdateSupplier() throws Exception {
        Supplier supplier = new Supplier("sup1", "Supplier A", "contact@suppliera.com", 
                                        "+33123456789", "France", "Île-de-France");
        supplierRepository.save(supplier);
        
        String updateJson = """
            {
                "name": "Updated Supplier A",
                "contactEmail": "newemail@suppliera.com",
                "contactPhone": "+33111111111",
                "country": "Germany",
                "region": "Berlin"
            }
            """;
        
        mockMvc.perform(put("/api/suppliers/sup1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Supplier A"))
                .andExpect(jsonPath("$.country").value("Germany"));
    }
    
    @Test
    void shouldDeleteSupplier() throws Exception {
        Supplier supplier = new Supplier("sup1", "Supplier A", "contact@suppliera.com", 
                                        "+33123456789", "France", "Île-de-France");
        supplierRepository.save(supplier);
        
        mockMvc.perform(delete("/api/suppliers/sup1"))
                .andExpect(status().isNoContent());
        
        mockMvc.perform(get("/api/suppliers/sup1"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldCascadeDeleteProductsWhenSupplierDeleted() throws Exception {
        Supplier supplier = new Supplier("sup1", "Supplier A", "contact@suppliera.com", 
                                        "+33123456789", "France", "Île-de-France");
        supplierRepository.save(supplier);
        
        Product product = new Product();
        product.setId("prod1");
        product.setName("Test Product");
        product.setSlug("test-product");
        product.setSupplier(supplier);
        productRepository.save(product);
        
        mockMvc.perform(delete("/api/suppliers/sup1"))
                .andExpect(status().isNoContent());
        
        mockMvc.perform(get("/api/products/prod1"))
                .andExpect(status().isNotFound());
    }
}
