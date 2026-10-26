package com.omniproduct.service;

import com.omniproduct.model.NonExpiring;
import com.omniproduct.model.Perishable;
import com.omniproduct.model.Product;
import com.omniproduct.model.ShelfLife;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Exercice étape 2 (HA4.8) : les libellés du catalogue se remplissent à la main, dans une boucle.
public class CatalogService {

    public List<String> catalogLabels(List<Product> products, LocalDate today) {
        List<String> labels = new ArrayList<>();
        for (Product product : products) {
            if (isPastSellBy(product.getShelfLife(), today)) {
                continue;
            }
            String label = product.getName();
            if (isPastBestBefore(product.getShelfLife(), today)) {
                label = label + " [BEST BEFORE PASSED]";
            }
            labels.add(label);
        }
        return labels;
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
