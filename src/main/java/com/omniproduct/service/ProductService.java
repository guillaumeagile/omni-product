package com.omniproduct.service;

import com.omniproduct.model.ProductStock;

// Exercice étape 2 (HA4.7, avertissement) : la vente retire du stock avant tout contrôle de vente.
public class ProductService {

    public ProductStock sellOne(ProductStock stock) {
        return withdrawStock(stock);
    }

    private ProductStock withdrawStock(ProductStock stock) {
        return new ProductStock(stock.stock() - 1, stock.quantity(), stock.warehouse());
    }
}
