package com.omniproduct.domain.port;

import com.omniproduct.model.Supplier;

import java.util.List;

public interface SupplierRepository extends Store<Supplier> {

    List<Supplier> findByAddressCountry(String country);

    List<Supplier> findByAddressRegion(String region);
}
