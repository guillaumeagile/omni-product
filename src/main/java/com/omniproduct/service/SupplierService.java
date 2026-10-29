package com.omniproduct.service;

import com.omniproduct.model.Contact;
import com.omniproduct.model.SupplierRegion;
import com.omniproduct.model.Supplier;
import com.omniproduct.model.Product;
import com.omniproduct.repository.SupplierRepository;
import com.omniproduct.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    
    public SupplierService(SupplierRepository supplierRepository, ProductRepository productRepository) {
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
    }
    
    public Supplier createSupplier(String id, String name, String contactEmail, String contactPhone,
                                   String country, String region) {
        Supplier supplier = new Supplier(id, name, contactEmail, contactPhone, country, region);
        return supplierRepository.save(supplier);
    }
    
    public Optional<Supplier> getSupplier(String id) {
        return supplierRepository.findById(id);
    }
    
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Supplier> getSuppliersByProduct(String productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (product.getSupply() != null && product.getSupply().getSupplier() != null) {
            return List.of(product.getSupply().getSupplier());
        }
        return List.of();
    }
    
    public List<Supplier> getSuppliersByCountry(String country) {
        return supplierRepository.findBySupplierRegionCountry(country);
    }

    public List<Supplier> getSuppliersByRegion(String region) {
        return supplierRepository.findBySupplierRegionRegion(region);
    }
    
    public Supplier updateSupplier(String id, String name, String contactEmail, String contactPhone,
                                   String country, String region) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        
        supplier.setName(name);
        supplier.setContact(new Contact(contactEmail, contactPhone));
        supplier.setSupplierRegion(new SupplierRegion(country, region));
        
        return supplierRepository.save(supplier);
    }
    
    @Transactional
    public void deleteSupplier(String id) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        List<Product> products = productRepository.findAll().stream()
            .filter(p -> p.getSupply() != null && p.getSupply().getSupplier() != null && id.equals(p.getSupply().getSupplier().getId()))
            .toList();
        productRepository.deleteAll(products);
        supplierRepository.delete(supplier);
    }
}
