package com.omniproduct.service;

import com.omniproduct.model.Location;
import com.omniproduct.model.Product;
import com.omniproduct.model.ProductStock;
import com.omniproduct.model.Quantity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceTest {

    private static final Product.Warehouse BORDEAUX = new Product.Warehouse(new Location("Bordeaux"));

    private final ProductService service = new ProductService();

    @Test
    void sellingOneTakesOneUnitOutOfTheStock() {
        ProductStock sold = service.sellOne(new ProductStock(new Quantity(5), new Quantity(12), BORDEAUX));

        assertThat(sold.stock()).isEqualTo(new Quantity(4));
    }

    @Test
    void sellingOneKeepsTheQuantityAndTheWarehouse() {
        ProductStock sold = service.sellOne(new ProductStock(new Quantity(5), new Quantity(12), BORDEAUX));

        assertThat(sold.quantity()).isEqualTo(new Quantity(12));
        assertThat(sold.warehouse()).isEqualTo(BORDEAUX);
    }

    @Test
    void sellingOneLeavesTheOriginalStockUntouched() {
        ProductStock before = new ProductStock(new Quantity(5), new Quantity(12), BORDEAUX);

        service.sellOne(before);

        assertThat(before.stock()).isEqualTo(new Quantity(5));
    }
}
