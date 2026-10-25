package com.omniproduct.controller;

import com.omniproduct.model.ProductTransport;
import com.omniproduct.repository.ProductRepository;
import com.omniproduct.repository.ProductTransportRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Bounded context Transport : poids et volume du produit.
@RestController
@RequestMapping("/api/products/{id}/transport")
public class ProductTransportController {

    private final ProductRepository productRepository;
    private final ProductTransportRepository transportRepository;

    public ProductTransportController(ProductRepository productRepository, ProductTransportRepository transportRepository) {
        this.productRepository = productRepository;
        this.transportRepository = transportRepository;
    }

    @GetMapping
    public ResponseEntity<ProductTransport> getTransport(@PathVariable String id) {
        return transportRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<ProductTransport> updateTransport(@PathVariable String id, @RequestBody ProductTransport transport) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(transportRepository.save(
                new ProductTransport(id, transport.getKilos(), transport.getVolume())));
    }
}
