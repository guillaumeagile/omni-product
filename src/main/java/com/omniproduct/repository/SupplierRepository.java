package com.omniproduct.repository;

import com.omniproduct.model.Supplier;
import com.omniproduct.model.Country;
import com.omniproduct.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {
    List<Supplier> findByCountry(Country country);
    List<Supplier> findByRegion(Region region);
    Optional<Supplier> findByName(String name);
}
