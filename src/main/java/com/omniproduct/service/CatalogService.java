package com.omniproduct.service;

import com.omniproduct.model.NonExpiring;
import com.omniproduct.model.Perishable;
import com.omniproduct.model.Product;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Exercice phase 3 (HA14) : les libellés du catalogue se remplissent à la main, dans une boucle.
public class CatalogService {

    public List<String> catalogLabels(List<Product> products, LocalDate today) {
        List<String> labels = new ArrayList<>();
        for (Product product : products) {
            if (!product.canSellOn(today)) {
                continue;
            }
            labels.add(label(product, today));
        }
        return labels;
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
