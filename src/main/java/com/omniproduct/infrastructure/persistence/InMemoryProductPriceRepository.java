package com.omniproduct.infrastructure.persistence;

import com.omniproduct.domain.port.ProductPriceRepository;
import com.omniproduct.model.Product;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryProductPriceRepository extends InMemoryStore<Product.Price> implements ProductPriceRepository {
}
