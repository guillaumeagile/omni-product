import {describe, expect, it} from 'vitest';
import {StockItem} from './stock-item';
import {StockDepleted} from './events/stock-depleted';

describe('StockItem#reserve', () => {
    it('records exactly one StockDepleted, with the right productId, when reserving the last unit', () => {
        const stockItem = StockItem.create({productId: 'product-1', availableQuantity: 1})._unsafeUnwrap();

        const result = stockItem.reserve(1);

        expect(result.isOk()).toBe(true);
        const events = stockItem.pullDomainEvents();
        expect(events).toHaveLength(1);
        expect(events[0]).toBeInstanceOf(StockDepleted);
        expect((events[0] as StockDepleted).productId).toBe('product-1');
    });

    it('records no event when reserving with stock remaining', () => {
        const stockItem = StockItem.create({productId: 'product-1', availableQuantity: 5})._unsafeUnwrap();

        const result = stockItem.reserve(2);

        expect(result.isOk()).toBe(true);
        expect(stockItem.pullDomainEvents()).toEqual([]);
    });

    it('records no event when a reservation fails for insufficient stock', () => {
        const stockItem = StockItem.create({productId: 'product-1', availableQuantity: 1})._unsafeUnwrap();

        const result = stockItem.reserve(2);

        expect(result.isErr()).toBe(true);
        expect(stockItem.pullDomainEvents()).toEqual([]);
    });

    it('returns Err with the requested and available quantities when stock is insufficient', () => {
        const stockItem = StockItem.create({productId: 'product-1', availableQuantity: 1})._unsafeUnwrap();

        const result = stockItem.reserve(2);

        expect(result._unsafeUnwrapErr()).toEqual({
            kind: 'InsufficientStockNotEnoughAvailable',
            requested: 2,
            available: 1,
        });
    });
});

describe('StockItem#pullDomainEvents', () => {
    it('drains recorded events — a second call returns an empty array', () => {
        const stockItem = StockItem.create({productId: 'product-1', availableQuantity: 1})._unsafeUnwrap();
        stockItem.reserve(1);

        stockItem.pullDomainEvents();
        const secondPull = stockItem.pullDomainEvents();

        expect(secondPull).toEqual([]);
    });
});
