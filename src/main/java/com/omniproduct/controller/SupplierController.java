package com.omniproduct.controller;

import com.omniproduct.model.Supplier;
import com.omniproduct.service.SupplierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {
    private final SupplierService supplierService;
    
    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }
    
    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@RequestBody SupplierRequest request) {
        Supplier supplier = supplierService.createSupplier(
            request.id(),
            request.name(),
            request.contactEmail(),
            request.contactPhone(),
            request.country(),
            request.region()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(supplier);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplier(@PathVariable String id) {
        Optional<Supplier> supplier = supplierService.getSupplier(id);
        return supplier.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
    }
    
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Supplier>> getSuppliersByProduct(@PathVariable String productId) {
        List<Supplier> suppliers = supplierService.getSuppliersByProduct(productId);
        return ResponseEntity.ok(suppliers);
    }
    
    @GetMapping("/country/{country}")
    public ResponseEntity<List<Supplier>> getSuppliersByCountry(@PathVariable String country) {
        List<Supplier> suppliers = supplierService.getSuppliersByCountry(country);
        return ResponseEntity.ok(suppliers);
    }
    
    @GetMapping("/region/{region}")
    public ResponseEntity<List<Supplier>> getSuppliersByRegion(@PathVariable String region) {
        List<Supplier> suppliers = supplierService.getSuppliersByRegion(region);
        return ResponseEntity.ok(suppliers);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable String id, @RequestBody SupplierUpdateRequest request) {
        Supplier supplier = supplierService.updateSupplier(
            id,
            request.name(),
            request.contactEmail(),
            request.contactPhone(),
            request.country(),
            request.region()
        );
        return ResponseEntity.ok(supplier);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable String id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
    
    public record SupplierRequest(
        String id,
        String name,
        String contactEmail,
        String contactPhone,
        String country,
        String region
    ) {}
    
    public record SupplierUpdateRequest(
        String name,
        String contactEmail,
        String contactPhone,
        String country,
        String region
    ) {}
}
