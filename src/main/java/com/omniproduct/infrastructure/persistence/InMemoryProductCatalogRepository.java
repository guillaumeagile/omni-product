package com.omniproduct.infrastructure.persistence;

import com.omniproduct.domain.port.ProductCatalogRepository;
import com.omniproduct.model.ProductCatalog;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryProductCatalogRepository extends InMemoryStore<ProductCatalog> implements ProductCatalogRepository {
}
