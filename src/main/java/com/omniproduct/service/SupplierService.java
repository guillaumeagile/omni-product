package com.omniproduct.service;

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
        productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return supplierRepository.findAll().stream()
            .filter(s -> s.getProducts() != null && s.getProducts().stream()
                .anyMatch(p -> p.getId().equals(productId)))
            .toList();
    }
    
    public List<Supplier> getSuppliersByCountry(String country) {
        return supplierRepository.findByCountry(country);
    }
    
    public List<Supplier> getSuppliersByRegion(String region) {
        return supplierRepository.findByRegion(region);
    }
    
    public Supplier updateSupplier(String id, String name, String contactEmail, String contactPhone,
                                   String country, String region) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        
        supplier.setName(name);
        supplier.setContactEmail(contactEmail);
        supplier.setContactPhone(contactPhone);
        supplier.setCountry(country);
        supplier.setRegion(region);
        
        return supplierRepository.save(supplier);
    }
    
    public void deleteSupplier(String id) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        supplierRepository.delete(supplier);
    }
}
