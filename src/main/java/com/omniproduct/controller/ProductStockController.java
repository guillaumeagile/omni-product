package com.omniproduct.controller;

import com.omniproduct.domain.port.ProductStockRepository;
import com.omniproduct.domain.port.ProductRepository;
import com.omniproduct.model.ProductStock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Bounded context Storage : combien d'unités, et où elles sont entreposées.
@RestController
@RequestMapping("/api/products/{id}/stock")
public class ProductStockController {

    private final ProductRepository productRepository;
    private final ProductStockRepository stockRepository;

    public ProductStockController(ProductRepository productRepository, ProductStockRepository stockRepository) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
    }

    @GetMapping
    public ResponseEntity<ProductStock> getStock(@PathVariable String id) {
        return stockRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<ProductStock> updateStock(@PathVariable String id, @RequestBody ProductStock value) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        stockRepository.save(id, value);
        return ResponseEntity.ok(value);
    }
}
