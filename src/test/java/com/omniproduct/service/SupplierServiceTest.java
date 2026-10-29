package com.omniproduct.service;

import com.omniproduct.model.Supplier;
import com.omniproduct.model.Product;
import com.omniproduct.repository.SupplierRepository;
import com.omniproduct.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class SupplierServiceTest {
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        supplierRepository.deleteAll();
    }
    
    @Test
    void shouldCreateSupplier() {
        Supplier supplier = supplierService.createSupplier(
            "sup1", "Supplier A", "contact@suppliera.com", 
            "+33123456789", "France", "Île-de-France"
        );
        
        assertThat(supplier).isNotNull();
        assertThat(supplier.getId()).isEqualTo("sup1");
        assertThat(supplier.getName()).isEqualTo("Supplier A");
        assertThat(supplier.getSupplierRegion().getCountry()).isEqualTo("France");
    }
    
    @Test
    void shouldGetSupplierById() {
        Supplier created = supplierService.createSupplier(
            "sup1", "Supplier A", "contact@suppliera.com", 
            "+33123456789", "France", "Île-de-France"
        );
        
        Optional<Supplier> found = supplierService.getSupplier("sup1");
        
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("sup1");
        assertThat(found.get().getName()).isEqualTo("Supplier A");
    }
    
    @Test
    void shouldReturnEmptyOptionalForNonExistentSupplier() {
        Optional<Supplier> found = supplierService.getSupplier("nonexistent");
        
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldGetAllSuppliers() {
        supplierService.createSupplier("sup1", "Supplier A", "contact@suppliera.com", 
                                      "+33123456789", "France", "Île-de-France");
        supplierService.createSupplier("sup2", "Supplier B", "contact@supplierb.com", 
                                      "+33987654321", "Spain", "Madrid");
        
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        
        assertThat(suppliers).hasSize(2);
        assertThat(suppliers).extracting("name").contains("Supplier A", "Supplier B");
    }
    
    @Test
    void shouldGetSuppliersByCountry() {
        supplierService.createSupplier("sup1", "Supplier A", "contact@suppliera.com", 
                                      "+33123456789", "France", "Île-de-France");
        supplierService.createSupplier("sup2", "Supplier B", "contact@supplierb.com", 
                                      "+33987654321", "Spain", "Madrid");
        
        List<Supplier> frenchSuppliers = supplierService.getSuppliersByCountry("France");
        
        assertThat(frenchSuppliers).hasSize(1);
        assertThat(frenchSuppliers.get(0).getName()).isEqualTo("Supplier A");
    }
    
    @Test
    void shouldGetSuppliersByRegion() {
        supplierService.createSupplier("sup1", "Supplier A", "contact@suppliera.com", 
                                      "+33123456789", "France", "Île-de-France");
        supplierService.createSupplier("sup2", "Supplier B", "contact@supplierb.com", 
                                      "+33987654321", "France", "Provence");
        
        List<Supplier> idfSuppliers = supplierService.getSuppliersByRegion("Île-de-France");
        
        assertThat(idfSuppliers).hasSize(1);
        assertThat(idfSuppliers.get(0).getName()).isEqualTo("Supplier A");
    }
    
    @Test
    void shouldUpdateSupplier() {
        supplierService.createSupplier("sup1", "Supplier A", "contact@suppliera.com", 
                                      "+33123456789", "France", "Île-de-France");
        
        Supplier updated = supplierService.updateSupplier(
            "sup1", "Updated Supplier A", "newemail@suppliera.com", 
            "+33111111111", "Germany", "Berlin"
        );
        
        assertThat(updated.getName()).isEqualTo("Updated Supplier A");
        assertThat(updated.getSupplierRegion().getCountry()).isEqualTo("Germany");
        assertThat(updated.getSupplierRegion().getRegion()).isEqualTo("Berlin");
    }
    
    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentSupplier() {
        assertThatThrownBy(() -> 
            supplierService.updateSupplier("nonexistent", "Name", "email@test.com", 
                                          "+33123456789", "France", "Île-de-France")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Supplier not found");
    }
    
    @Test
    void shouldDeleteSupplier() {
        supplierService.createSupplier("sup1", "Supplier A", "contact@suppliera.com", 
                                      "+33123456789", "France", "Île-de-France");
        
        supplierService.deleteSupplier("sup1");
        
        Optional<Supplier> found = supplierService.getSupplier("sup1");
        assertThat(found).isEmpty();
    }
    
    @Test
    void shouldThrowExceptionWhenDeletingNonExistentSupplier() {
        assertThatThrownBy(() -> supplierService.deleteSupplier("nonexistent"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Supplier not found");
    }
    
    @Test
    void shouldCascadeDeleteProductsWhenSupplierDeleted() {
        Supplier supplier = supplierService.createSupplier(
            "sup1", "Supplier A", "contact@suppliera.com", 
            "+33123456789", "France", "Île-de-France"
        );
        
        Product product = new Product("prod1", "Test Product", "test-product", null, null, null, null, null, null, null, null, null);
        product.getSupply().setSupplier(supplier);
        productRepository.save(product);
        
        assertThat(productRepository.findById("prod1")).isPresent();
        
        supplierService.deleteSupplier("sup1");
        
        assertThat(productRepository.findById("prod1")).isEmpty();
    }
    
    @Test
    @Transactional
    void shouldGetSuppliersByProduct() {
        Supplier supplier = supplierService.createSupplier(
            "sup1", "Supplier A", "contact@suppliera.com", 
            "+33123456789", "France", "Île-de-France"
        );
        
        Product product = new Product("prod1", "Test Product", "test-product", null, null, null, null, null, null, null, null, null);
        product.getSupply().setSupplier(supplier);
        productRepository.save(product);
        
        List<Supplier> suppliers = supplierService.getSuppliersByProduct("prod1");
        
        assertThat(suppliers).hasSize(1);
        assertThat(suppliers.get(0).getId()).isEqualTo("sup1");
    }
}
