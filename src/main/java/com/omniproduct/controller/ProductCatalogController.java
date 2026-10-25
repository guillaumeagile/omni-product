package com.omniproduct.controller;

import com.omniproduct.model.ProductCatalog;
import com.omniproduct.repository.ProductCatalogRepository;
import com.omniproduct.repository.ProductRepository;
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
    public ResponseEntity<ProductCatalog> updateCatalog(@PathVariable String id, @RequestBody ProductCatalog catalog) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(catalogRepository.save(
                new ProductCatalog(id, catalog.getSlug(), catalog.getDiscounts(), catalog.getImages())));
    }
}
