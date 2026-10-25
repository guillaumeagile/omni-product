package com.omniproduct.service;

import com.omniproduct.model.ProductStock;
import com.omniproduct.model.Quantity;

// Exercice étape 2 (HA4.7, avertissement) : la vente retire du stock avant tout contrôle de vente.
public class ProductService {

    public ProductStock sellOne(ProductStock stock) {
        return withdrawStock(stock);
    }

    private ProductStock withdrawStock(ProductStock stock) {
        return new ProductStock(new Quantity(stock.stock().value() - 1), stock.quantity(), stock.warehouse());
    }
}
