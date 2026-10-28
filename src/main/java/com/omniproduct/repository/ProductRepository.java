package com.omniproduct.repository;

import com.omniproduct.model.Product;
import com.omniproduct.model.SupplierId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    Optional<Product> findByCatalogSlug(String slug);

    boolean existsByCatalogSlug(String slug);

    void deleteByCatalogSlug(String slug);

    void deleteBySupplySupplierId(SupplierId supplierId);
}
