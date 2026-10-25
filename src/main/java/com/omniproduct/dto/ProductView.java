package com.omniproduct.dto;

import com.omniproduct.model.Product;

import java.util.List;
import java.util.Map;

// La forme à plat d'un produit, telle que l'API la reçoit et la renvoie. Elle assemble les entités par préoccupation.
public record ProductView(
        String id,
        String name,
        String slug,
        Product.Price price,
        List<String> discounts,
        Map<String, String> images,
        Map<String, Product.Supplier> suppliersRegions,
        Double kilos,
        String volume,
        Integer quantity,
        Integer stock,
        Product.Warehouse warehouse,
        String supplierId) {

    public ProductView withId(String newId) {
        return new ProductView(newId, name, slug, price, discounts, images, suppliersRegions,
                kilos, volume, quantity, stock, warehouse, supplierId);
    }
}
