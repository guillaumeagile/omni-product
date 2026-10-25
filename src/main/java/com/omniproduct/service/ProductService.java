package com.omniproduct.service;

import com.omniproduct.model.*;
import com.omniproduct.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Le cycle de vie du produit : son identité. Les autres préoccupations sont exposées par leurs propres routes.
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductCatalogRepository catalogRepository;
    private final ProductPriceRepository priceRepository;
    private final ProductStockRepository stockRepository;
    private final ProductTransportRepository transportRepository;
    private final ProductSourcingRepository sourcingRepository;

    public ProductService(ProductRepository productRepository, ProductCatalogRepository catalogRepository,
                          ProductPriceRepository priceRepository, ProductStockRepository stockRepository,
                          ProductTransportRepository transportRepository, ProductSourcingRepository sourcingRepository) {
        this.productRepository = productRepository;
        this.catalogRepository = catalogRepository;
        this.priceRepository = priceRepository;
        this.stockRepository = stockRepository;
        this.transportRepository = transportRepository;
        this.sourcingRepository = sourcingRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(String id) {
        return productRepository.findById(id);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    // Supprimer un produit supprime aussi ses préoccupations : elles n'ont pas de sens sans lui.
    @Transactional
    public void deleteById(String id) {
        productRepository.deleteById(id);
        catalogRepository.findById(id).ifPresent(catalogRepository::delete);
        priceRepository.findById(id).ifPresent(priceRepository::delete);
        stockRepository.findById(id).ifPresent(stockRepository::delete);
        transportRepository.findById(id).ifPresent(transportRepository::delete);
        sourcingRepository.findById(id).ifPresent(sourcingRepository::delete);
    }
}
