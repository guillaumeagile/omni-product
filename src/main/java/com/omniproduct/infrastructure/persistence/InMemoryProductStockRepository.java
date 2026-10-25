package com.omniproduct.infrastructure.persistence;

import com.omniproduct.domain.port.ProductStockRepository;
import com.omniproduct.model.ProductStock;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryProductStockRepository extends InMemoryStore<ProductStock> implements ProductStockRepository {
}
