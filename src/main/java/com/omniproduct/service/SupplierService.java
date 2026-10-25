package com.omniproduct.service;

import com.omniproduct.domain.port.ProductRepository;
import com.omniproduct.domain.port.ProductSourcingRepository;
import com.omniproduct.domain.port.SupplierRepository;
import com.omniproduct.model.ProductSourcing;
import com.omniproduct.model.Supplier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ProductSourcingRepository productSourcingRepository;
    private final ProductService productService;

    public SupplierService(SupplierRepository supplierRepository, ProductRepository productRepository,
                           ProductSourcingRepository productSourcingRepository, ProductService productService) {
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.productSourcingRepository = productSourcingRepository;
        this.productService = productService;
    }

    public Supplier createSupplier(String id, String name, String contactEmail, String contactPhone,
                                   String country, String region) {
        Supplier supplier = new Supplier(id, name, contactEmail, contactPhone, country, region);
        supplierRepository.save(id, supplier);
        return supplier;
    }

    public Optional<Supplier> getSupplier(String id) {
        return supplierRepository.findById(id);
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public List<Supplier> getSuppliersByProduct(String productId) {
        productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return productSourcingRepository.findById(productId)
            .map(ProductSourcing::getSupplierId)
            .flatMap(supplierRepository::findById)
            .stream()
            .toList();
    }

    public List<Supplier> getSuppliersByCountry(String country) {
        return supplierRepository.findByAddressCountry(country);
    }

    public List<Supplier> getSuppliersByRegion(String region) {
        return supplierRepository.findByAddressRegion(region);
    }

    public Supplier updateSupplier(String id, String name, String contactEmail, String contactPhone,
                                   String country, String region) {
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));

        supplier.setName(name);
        supplier.setContact(new Supplier.Contact(contactEmail, contactPhone));
        supplier.setAddress(new Supplier.Address(country, region));
        supplierRepository.save(id, supplier);
        return supplier;
    }

    public void deleteSupplier(String id) {
        supplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        // Les produits de ce fournisseur disparaissent avec lui.
        productSourcingRepository.findProductIdsBySupplierId(id).forEach(productService::deleteById);
        supplierRepository.deleteById(id);
    }
}
