import {describe, expect, it} from 'vitest';
import {StockItem} from './stock-item';

// StockItem is an immutable aggregate: reserve() never mutates the instance it's
// called on — it returns a NEW StockItem. Object.freeze on the original instance
// makes that contract undeniable: any test that accidentally relied on in-place
// mutation would throw here, in strict mode, rather than passing by coincidence.
const frozenStockItem = (props: { productId: string; availableQuantity: number }): StockItem =>
    Object.freeze(StockItem.create(props)._unsafeUnwrap());

describe('StockItem#reserve — business state', () => {
    it('the returned StockItem has availableQuantity decremented by the reserved quantity', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 5});

        const result = original.reserve(2);

        expect(result._unsafeUnwrap().availableQuantity).toBe(3);
    });

    it('the original StockItem keeps its availableQuantity unchanged after a successful reserve', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 5});

        original.reserve(2);

        expect(original.availableQuantity).toBe(5);
    });

    it('the original StockItem keeps its availableQuantity unchanged when a reservation fails for insufficient stock', () => {
        const original = frozenStockItem({productId: 'product-1', availableQuantity: 1});

        const result = original.reserve(2);

        expect(result.isErr()).toBe(true);
        expect(original.availableQuantity).toBe(1);
    });
});
