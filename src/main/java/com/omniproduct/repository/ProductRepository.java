package com.omniproduct.repository;

import com.omniproduct.model.Product;
import com.omniproduct.model.Slug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findBySlug(Slug slug);

    boolean existsBySlug(Slug slug);

    void deleteBySlug(Slug slug);
}
