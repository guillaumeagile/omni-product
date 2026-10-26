package com.omniproduct.service;

import com.omniproduct.model.Product;
import com.omniproduct.model.ProductStock;
import com.omniproduct.model.Quantity;
import io.vavr.control.Option;

import java.time.LocalDate;

public class ProductService {

    public Option<ProductStock> sellOne(Product product, ProductStock stock, LocalDate today) {
        if (!product.canSellOn(today)) {
            return Option.none();
        }
        return Option.of(withdrawStock(stock));
    }

    private ProductStock withdrawStock(ProductStock stock) {
        return new ProductStock(new Quantity(stock.stock().value() - 1), stock.quantity(), stock.warehouse());
    }
}
