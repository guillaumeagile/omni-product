package com.omniproduct.repository;

import com.omniproduct.model.ProductSourcing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSourcingRepository extends JpaRepository<ProductSourcing, String> {

    @Query("select s.productId from ProductSourcing s where s.supplierId = :supplierId")
    List<String> findProductIdsBySupplierId(@Param("supplierId") String supplierId);
}
