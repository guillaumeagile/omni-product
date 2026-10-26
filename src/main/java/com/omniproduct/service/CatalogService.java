package com.omniproduct.service;

import com.omniproduct.model.NonExpiring;
import com.omniproduct.model.Perishable;
import com.omniproduct.model.Product;

import java.time.LocalDate;
import java.util.List;

public class CatalogService {

    public List<String> catalogLabels(List<Product> products, LocalDate today) {
        return products.stream()
                .filter(product -> product.canSellOn(today))
                .map(product -> label(product, today))
                .toList();
    }

    private String label(Product product, LocalDate today) {
        return switch (product.getShelfLife()) {
            case Perishable perishable -> product.getName();
            case NonExpiring nonExpiring -> today.isAfter(nonExpiring.bestBeforeDate())
                    ? product.getName() + " [BEST BEFORE PASSED]"
                    : product.getName();
        };
    }
}
