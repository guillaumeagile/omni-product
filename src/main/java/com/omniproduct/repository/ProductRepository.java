package com.omniproduct.repository;

import com.omniproduct.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    
    Optional<Product> findBySlug(String slug);
    
    boolean existsBySlug(String slug);
    
    void deleteBySlug(String slug);
}
