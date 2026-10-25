package com.omniproduct.infrastructure.persistence;

import com.omniproduct.domain.port.ProductTransportRepository;
import com.omniproduct.model.ProductTransport;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryProductTransportRepository extends InMemoryStore<ProductTransport> implements ProductTransportRepository {
}
