package com.omniproduct.repository;

import com.omniproduct.model.ProductTransport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductTransportRepository extends JpaRepository<ProductTransport, String> {
}
