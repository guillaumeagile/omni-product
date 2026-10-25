package com.omniproduct.infrastructure.persistence;

import com.omniproduct.domain.port.SupplierRepository;
import com.omniproduct.model.Supplier;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InMemorySupplierRepository extends InMemoryStore<Supplier> implements SupplierRepository {

    @Override
    public List<Supplier> findByAddressCountry(String country) {
        return values.values().stream()
                .filter(supplier -> supplier.getAddress() != null && country.equals(supplier.getAddress().getCountry()))
                .toList();
    }

    @Override
    public List<Supplier> findByAddressRegion(String region) {
        return values.values().stream()
                .filter(supplier -> supplier.getAddress() != null && region.equals(supplier.getAddress().getRegion()))
                .toList();
    }
}
