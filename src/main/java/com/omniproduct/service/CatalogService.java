package com.omniproduct.service;

import com.omniproduct.model.NonExpiring;
import com.omniproduct.model.Perishable;
import com.omniproduct.model.Product;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Exercice étape 2 (HA4.8) : les libellés du catalogue se remplissent à la main, dans une boucle.
public class CatalogService {

    public List<String> catalogLabels(List<Product> products, LocalDate today) {
        List<String> labels = new ArrayList<>();
        for (Product product : products) {
            if (product.getShelfLife() instanceof Perishable perishable && today.isAfter(perishable.sellByDate())) {
                continue;
            }
            String label = product.getName();
            if (product.getShelfLife() instanceof NonExpiring nonExpiring && today.isAfter(nonExpiring.bestBeforeDate())) {
                label = label + " [BEST BEFORE PASSED]";
            }
            labels.add(label);
        }
        return labels;
    }
}
