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
            if (product instanceof Perishable perishable
                    && today.isAfter(perishable.getShelfLife().sellByDate())) {
                continue;
            }
            String label = product.getName();
            if (product instanceof NonExpiring nonExpiring && today.isAfter(nonExpiring.getBestBeforeDate())) {
                label = label + " [BEST BEFORE PASSED]";
            }
            labels.add(label);
        }
        return labels;
    }
}
