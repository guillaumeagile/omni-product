package com.omniproduct.service;

import com.omniproduct.model.Product;
import com.omniproduct.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(String id) {
        return productRepository.findById(id);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void deleteById(String id) {
        productRepository.deleteById(id);
    }

    // Vente d'un produit : le retrait de stock vient avant tout contrôle de vente (HA4.7, avertissement).
    public void sellOne(Product product) {
        withdrawStock(product);
    }

    private void withdrawStock(Product product) {
        product.setStock(product.getStock() - 1);
    }
}
