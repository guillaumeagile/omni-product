package com.omniproduct.model;

// Le stock : combien d'unités, et où elles sont entreposées.
public record ProductStock(Integer stock, Integer quantity, Product.Warehouse warehouse) {}
