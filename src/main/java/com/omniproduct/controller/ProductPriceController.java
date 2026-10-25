package com.omniproduct.controller;

import com.omniproduct.model.Product;
import com.omniproduct.model.ProductPrice;
import com.omniproduct.repository.ProductPriceRepository;
import com.omniproduct.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Bounded context Pricing : le prix du produit.
@RestController
@RequestMapping("/api/products/{id}/price")
public class ProductPriceController {

    private final ProductRepository productRepository;
    private final ProductPriceRepository priceRepository;

    public ProductPriceController(ProductRepository productRepository, ProductPriceRepository priceRepository) {
        this.productRepository = productRepository;
        this.priceRepository = priceRepository;
    }

    @GetMapping
    public ResponseEntity<Product.Price> getPrice(@PathVariable String id) {
        return priceRepository.findById(id)
                .map(ProductPrice::getPrice)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<Product.Price> updatePrice(@PathVariable String id, @RequestBody Product.Price price) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(priceRepository.save(new ProductPrice(id, price)).getPrice());
    }
}
