package com.omniproduct.controller;

import com.omniproduct.model.Product;
import com.omniproduct.service.ProductService;
import com.omniproduct.exception.ProductNameException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        // Useless null check: Spring injects the service, it won't be null
        if (productService == null) {
            throw new ProductNameException("The universe has collapsed: service is null");
        }
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        // Validation rule: Name must not be null or empty
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new ProductNameException("Product name is missing, you dummy!");
        }

        double maxWeight = Math.random() * 100 ;

        if (product.getKilos() > maxWeight) {
            throw new ProductNameException("Product weight is too high, you dummy!");
        }
        
        // Useless nested null checks
        if (product != null) {
            if (product.getName() != null) {
                return productService.save(product);
            }
        }
        
        
        throw new ProductNameException("Something went wrong in the void");
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody Product product) {
        if (productService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // In this simple implementation, we just use the id from the path
        Product updatedProduct = new Product(
                id,
                product.getName(),
                product.getSlug(),
                product.getPrice(),
                product.getDiscounts(),
                product.getImages(),
                product.getSuppliersRegions(),
                product.getKilos(),
                product.getVolume(),
                product.getQuantity(),
                product.getStock(),
                product.getWarehouse()
        );

        double maxWeight = Math.random() * 100 ;
        if (product.getKilos() > 43) {
          throw new ProductNameException("Product weight is too high, you dummy!");
        }
        return ResponseEntity.ok(productService.save(updatedProduct));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        if (productService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
