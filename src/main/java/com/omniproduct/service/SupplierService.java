package com.omniproduct.service;

import com.omniproduct.model.Supplier;
import com.omniproduct.model.Product;
import com.omniproduct.model.ContactEmail;
import com.omniproduct.model.ContactPhone;
import com.omniproduct.model.Country;
import com.omniproduct.model.Region;
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
        Supplier supplier = new Supplier(id, name, new ContactEmail(contactEmail), new ContactPhone(contactPhone),
                new Country(country), new Region(region));
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
        if (product.getSupplierId() == null) {
            return List.of();
        }
        return supplierRepository.findById(product.getSupplierId().value())
            .map(List::of)
            .orElse(List.of());
    }
    
    public List<Supplier> getSuppliersByCountry(String country) {
        return supplierRepository.findByCountry(new Country(country));
    }

    public List<Supplier> getSuppliersByRegion(String region) {
        return supplierRepository.findByRegion(new Region(region));
    }

    public Supplier updateSupplier(String id, String name, String contactEmail, String contactPhone,
                                   String country, String region) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));

        supplier.setName(name);
        supplier.setContactEmail(new ContactEmail(contactEmail));
        supplier.setContactPhone(new ContactPhone(contactPhone));
        supplier.setCountry(new Country(country));
        supplier.setRegion(new Region(region));

        return supplierRepository.save(supplier);
    }
    
    public void deleteSupplier(String id) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        supplierRepository.delete(supplier);
    }
}
