package com.omniproduct.service;

import com.omniproduct.dto.ProductView;
import com.omniproduct.model.*;
import com.omniproduct.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

    public List<ProductView> findAll() {
        return productRepository.findAll().stream().map(this::view).toList();
    }

    public Optional<ProductView> findById(String id) {
        return productRepository.findById(id).map(this::view);
    }

    @Transactional
    public ProductView save(ProductView product) {
        String id = product.id();
        productRepository.save(new Product(id, product.name()));
        catalogRepository.save(new ProductCatalog(id, product.slug(), product.discounts(), product.images()));
        priceRepository.save(new ProductPrice(id, product.price()));
        stockRepository.save(new ProductStock(id, product.stock(), product.quantity(), product.warehouse()));
        transportRepository.save(new ProductTransport(id, product.kilos(), product.volume()));
        sourcingRepository.save(new ProductSourcing(id, product.supplierId(), product.suppliersRegions()));
        return findById(id).orElseThrow();
    }

    @Transactional
    public void deleteById(String id) {
        productRepository.deleteById(id);
        catalogRepository.deleteById(id);
        priceRepository.deleteById(id);
        stockRepository.deleteById(id);
        transportRepository.deleteById(id);
        sourcingRepository.deleteById(id);
    }

    // Un produit est toujours enregistré avec ses six parties ; un morceau absent ne laisse que des champs vides.
    private ProductView view(Product product) {
        String id = product.getId();
        ProductCatalog catalog = catalogRepository.findById(id).orElseGet(ProductCatalog::new);
        ProductStock stock = stockRepository.findById(id).orElseGet(ProductStock::new);
        ProductTransport transport = transportRepository.findById(id).orElseGet(ProductTransport::new);
        ProductSourcing sourcing = sourcingRepository.findById(id).orElseGet(ProductSourcing::new);
        return new ProductView(id, product.getName(), catalog.getSlug(),
                priceRepository.findById(id).map(ProductPrice::getPrice).orElse(null),
                catalog.getDiscounts(), catalog.getImages(), sourcing.getSuppliersRegions(),
                transport.getKilos(), transport.getVolume(), stock.getQuantity(), stock.getStock(),
                stock.getWarehouse(), sourcing.getSupplierId());
    }
}
