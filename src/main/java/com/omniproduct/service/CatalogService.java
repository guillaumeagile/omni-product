package com.omniproduct.service;

import com.omniproduct.model.NonExpiring;
import com.omniproduct.model.Perishable;
import com.omniproduct.model.Product;
import com.omniproduct.model.ShelfLife;

import java.time.LocalDate;
import java.util.List;

// Exercice étape 2 (HA4.8) : les libellés du catalogue se remplissent à la main, dans une boucle.
public class CatalogService {

    public List<String> catalogLabels(List<Product> products, LocalDate today) {
        return products.stream()
                .filter(product -> !isPastSellBy(product.getShelfLife(), today))
                .map(product -> labelOf(product, today))
                .toList();
    }

    private static String labelOf(Product product, LocalDate today) {
        String label = product.getName();
        if (isPastBestBefore(product.getShelfLife(), today)) {
            label = label + " [BEST BEFORE PASSED]";
        }
        return label;
    }

    private static boolean isPastSellBy(ShelfLife shelfLife, LocalDate today) {
        return switch (shelfLife) {
            case Perishable perishable -> today.isAfter(perishable.sellByDate());
            case NonExpiring nonExpiring -> false;
        };
    }

    private static boolean isPastBestBefore(ShelfLife shelfLife, LocalDate today) {
        return switch (shelfLife) {
            case Perishable perishable -> false;
            case NonExpiring nonExpiring -> today.isAfter(nonExpiring.bestBeforeDate());
        };
    }
}
