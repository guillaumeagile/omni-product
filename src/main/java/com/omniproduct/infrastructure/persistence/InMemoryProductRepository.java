package com.omniproduct.infrastructure.persistence;

import com.omniproduct.domain.port.ProductRepository;
import com.omniproduct.model.Product;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryProductRepository extends InMemoryStore<Product> implements ProductRepository {
}
