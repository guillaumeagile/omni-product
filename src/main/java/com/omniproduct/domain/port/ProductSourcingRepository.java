package com.omniproduct.domain.port;

import com.omniproduct.model.ProductSourcing;

import java.util.List;

public interface ProductSourcingRepository extends Store<ProductSourcing> {

    List<String> findProductIdsBySupplierId(String supplierId);
}
