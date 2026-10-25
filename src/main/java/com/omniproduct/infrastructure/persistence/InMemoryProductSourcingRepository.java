package com.omniproduct.infrastructure.persistence;

import com.omniproduct.domain.port.ProductSourcingRepository;
import com.omniproduct.model.ProductSourcing;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InMemoryProductSourcingRepository extends InMemoryStore<ProductSourcing> implements ProductSourcingRepository {

    @Override
    public List<String> findProductIdsBySupplierId(String supplierId) {
        return values.entrySet().stream()
                .filter(entry -> supplierId.equals(entry.getValue().getSupplierId()))
                .map(entry -> entry.getKey())
                .toList();
    }
}
