package com.omniproduct.controller;

import com.omniproduct.domain.port.ProductCatalogRepository;
import com.omniproduct.domain.port.ProductRepository;
import com.omniproduct.model.ProductCatalog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Bounded context Catalog : comment le produit se présente.
@RestController
@RequestMapping("/api/products/{id}/catalog")
public class ProductCatalogController {

    private final ProductRepository productRepository;
    private final ProductCatalogRepository catalogRepository;

    public ProductCatalogController(ProductRepository productRepository, ProductCatalogRepository catalogRepository) {
        this.productRepository = productRepository;
        this.catalogRepository = catalogRepository;
    }

    @GetMapping
    public ResponseEntity<ProductCatalog> getCatalog(@PathVariable String id) {
        return catalogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<ProductCatalog> updateCatalog(@PathVariable String id, @RequestBody ProductCatalog value) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        catalogRepository.save(id, value);
        return ResponseEntity.ok(value);
    }
}
