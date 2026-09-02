import {describe, expect, it} from 'vitest';
import {StockItem} from './stock-item';
import {StockDepleted} from './events/stock-depleted';

// StockItem is an immutable aggregate: reserve() never mutates the instance it's
// called on — it returns a NEW StockItem. Object.freeze on the original instance
// makes that contract undeniable: any test that accidentally relied on in-place
// mutation would throw here, in strict mode, rather than passing by coincidence.
const frozenStockItem = (props: { productId: string; availableQuantity: number }): StockItem =>
    Object.freeze(StockItem.create(props)._unsafeUnwrap());

describe('StockItem#reserve', () => {
    it('returns a new StockItem and leaves the original instance untouched, when reserving the last unit', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 1});

        const result = original.reserve(1);

        expect(result._unsafeUnwrap().availableQuantity).toBe(0);
        expect(original.availableQuantity).toBe(1);
    });

    it('records exactly one StockDepleted, with the right productId, on the returned StockItem, when reserving the last unit', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 1});

        const reserved = original.reserve(1)._unsafeUnwrap();

        const events = reserved.pullDomainEvents();
        expect(events).toHaveLength(1);
        expect(events[0]).toBeInstanceOf(StockDepleted);
        expect((events[0] as StockDepleted).productId).toBe('product-1');
    });

    it('records no event on the original instance — it was never mutated, so it never recorded anything', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 1});

        original.reserve(1);

        expect(original.pullDomainEvents()).toEqual([]);
    });

    it('returns a new StockItem and leaves the original instance untouched, when reserving with stock remaining', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 5});

        const result = original.reserve(2);

        expect(result._unsafeUnwrap().availableQuantity).toBe(3);
        expect(original.availableQuantity).toBe(5);
    });

    it('records no event on the returned StockItem when reserving with stock remaining', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 5});

        const reserved = original.reserve(2)._unsafeUnwrap();

        expect(reserved.pullDomainEvents()).toEqual([]);
    });

    it('leaves the original instance untouched when a reservation fails for insufficient stock', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 1});

        const result = original.reserve(2);

        expect(result.isErr()).toBe(true);
        expect(original.availableQuantity).toBe(1);
    });

    it('records no event on the original instance when a reservation fails for insufficient stock', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 1});

        original.reserve(2);

        expect(original.pullDomainEvents()).toEqual([]);
    });

    it('returns Err with the requested and available quantities when stock is insufficient', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 1});

        const result = original.reserve(2);

        expect(result._unsafeUnwrapErr()).toEqual({
            kind: 'InsufficientStockNotEnoughAvailable',
            requested: 2,
            available: 1,
        });
    });
});

describe('StockItem#pullDomainEvents', () => {
    it('drains recorded events on the returned StockItem — a second call returns an empty array', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 1});
        const reserved = original.reserve(1)._unsafeUnwrap();

        reserved.pullDomainEvents();
        const secondPull = reserved.pullDomainEvents();

        expect(secondPull).toEqual([]);
    });
});
