package com.omniproduct.controller;

import com.omniproduct.domain.port.ProductSourcingRepository;
import com.omniproduct.domain.port.ProductRepository;
import com.omniproduct.model.ProductSourcing;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Bounded context Supplier : quel fournisseur approvisionne le produit, et par région.
@RestController
@RequestMapping("/api/products/{id}/sourcing")
public class ProductSourcingController {

    private final ProductRepository productRepository;
    private final ProductSourcingRepository sourcingRepository;

    public ProductSourcingController(ProductRepository productRepository, ProductSourcingRepository sourcingRepository) {
        this.productRepository = productRepository;
        this.sourcingRepository = sourcingRepository;
    }

    @GetMapping
    public ResponseEntity<ProductSourcing> getSourcing(@PathVariable String id) {
        return sourcingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<ProductSourcing> updateSourcing(@PathVariable String id, @RequestBody ProductSourcing value) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        sourcingRepository.save(id, value);
        return ResponseEntity.ok(value);
    }
}
