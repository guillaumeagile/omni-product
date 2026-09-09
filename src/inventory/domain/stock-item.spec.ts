import {describe, expect, it} from 'vitest';
import {StockItem} from './stock-item';
import {STOCK_ITEM_ERROR_KIND} from './stock-item-error';

const okStockItem = (availableQuantity: number, reservedQuantity?: number): StockItem => {
    const result = StockItem.create({productId: 'product-1', availableQuantity, reservedQuantity});
    return result._unsafeUnwrap();
};

describe('StockItem.create — validation', () => {
    it('builds a valid StockItem, defaulting reservedQuantity to 0', () => {
        const item = okStockItem(5);

        expect(item.productId).toBe('product-1');
        expect(item.availableQuantity).toBe(5);
        expect(item.reservedQuantity).toBe(0);
    });

    it('keeps a caller-supplied reservedQuantity', () => {
        expect(okStockItem(5, 3).reservedQuantity).toBe(3);
    });

    it('rejects an empty productId', () => {
        const result = StockItem.create({productId: '', availableQuantity: 5});

        expect(result._unsafeUnwrapErr().kind).toBe(STOCK_ITEM_ERROR_KIND.ProductIdEmpty);
    });

    it('rejects a whitespace-only productId', () => {
        const result = StockItem.create({productId: '   ', availableQuantity: 5});

        expect(result._unsafeUnwrapErr().kind).toBe(STOCK_ITEM_ERROR_KIND.ProductIdEmpty);
    });

    it('rejects a negative availableQuantity with a typed error', () => {
        const result = StockItem.create({productId: 'product-1', availableQuantity: -1});

        expect(result._unsafeUnwrapErr()).toEqual({
            kind: STOCK_ITEM_ERROR_KIND.AvailableQuantityNegative,
            availableQuantity: -1,
        });
    });

    it('rejects a non-integer availableQuantity with a typed error', () => {
        const result = StockItem.create({productId: 'product-1', availableQuantity: 2.5});

        expect(result._unsafeUnwrapErr()).toEqual({
            kind: STOCK_ITEM_ERROR_KIND.AvailableQuantityNotInteger,
            availableQuantity: 2.5,
        });
    });

    it('rejects a negative reservedQuantity', () => {
        const result = StockItem.create({productId: 'product-1', availableQuantity: 5, reservedQuantity: -2});

        expect(result._unsafeUnwrapErr().kind).toBe(STOCK_ITEM_ERROR_KIND.AvailableQuantityNegative);
    });
});

describe('StockItem#reserve — errors', () => {
    it('reports InsufficientStock with the requested and available amounts', () => {
        const result = okStockItem(1).reserve(2);

        expect(result._unsafeUnwrapErr()).toEqual({
            kind: STOCK_ITEM_ERROR_KIND.InsufficientStock,
            requested: 2,
            available: 1,
        });
    });

    it('reports QuantityNotPositive for a zero or negative quantity', () => {
        expect(okStockItem(5).reserve(0)._unsafeUnwrapErr()).toEqual({
            kind: STOCK_ITEM_ERROR_KIND.QuantityNotPositive,
            quantity: 0,
        });
        expect(okStockItem(5).reserve(-3)._unsafeUnwrapErr()).toEqual({
            kind: STOCK_ITEM_ERROR_KIND.QuantityNotPositive,
            quantity: -3,
        });
    });

    it('reports QuantityNotInteger for a fractional quantity', () => {
        expect(okStockItem(5).reserve(1.5)._unsafeUnwrapErr()).toEqual({
            kind: STOCK_ITEM_ERROR_KIND.QuantityNotInteger,
            quantity: 1.5,
        });
    });

    it('reserving exactly the available quantity succeeds and leaves zero available', () => {
        const result = okStockItem(4).reserve(4);

        expect(result._unsafeUnwrap().availableQuantity).toBe(0);
        expect(result._unsafeUnwrap().reservedQuantity).toBe(4);
    });
});

describe('StockItem#release — errors', () => {
    it('reports InsufficientReservation with the requested and reserved amounts', () => {
        const reserved = okStockItem(10).reserve(4)._unsafeUnwrap();

        expect(reserved.release(5)._unsafeUnwrapErr()).toEqual({
            kind: STOCK_ITEM_ERROR_KIND.InsufficientReservation,
            requested: 5,
            reserved: 4,
        });
    });

    it('reports QuantityNotPositive for a zero quantity', () => {
        expect(okStockItem(10).release(0)._unsafeUnwrapErr().kind).toBe(
            STOCK_ITEM_ERROR_KIND.QuantityNotPositive,
        );
    });

    it('reports QuantityNotInteger for a fractional quantity', () => {
        expect(okStockItem(10, 5).release(2.5)._unsafeUnwrapErr().kind).toBe(
            STOCK_ITEM_ERROR_KIND.QuantityNotInteger,
        );
    });

    it('releasing exactly the reserved quantity succeeds and leaves zero reserved', () => {
        const reserved = okStockItem(10).reserve(4)._unsafeUnwrap();

        const result = reserved.release(4);

        expect(result._unsafeUnwrap().availableQuantity).toBe(10);
        expect(result._unsafeUnwrap().reservedQuantity).toBe(0);
    });
});

describe('StockItem#restock — errors', () => {
    it('reports QuantityNotPositive for a zero quantity', () => {
        expect(okStockItem(10).restock(0)._unsafeUnwrapErr().kind).toBe(
            STOCK_ITEM_ERROR_KIND.QuantityNotPositive,
        );
    });

    it('reports QuantityNotInteger for a fractional quantity', () => {
        expect(okStockItem(10).restock(0.5)._unsafeUnwrapErr().kind).toBe(
            STOCK_ITEM_ERROR_KIND.QuantityNotInteger,
        );
    });
});
