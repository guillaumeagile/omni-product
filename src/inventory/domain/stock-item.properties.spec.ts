import {describe, it} from 'vitest';
import * as fc from 'fast-check';
import {StockItem} from './stock-item';

const okStockItem = (availableQuantity: number): StockItem => {
    const result = StockItem.create({productId: 'product-1', availableQuantity});
    if (result.isErr()) {
        throw new Error(`expected a valid StockItem for ${availableQuantity}`);
    }
    return result.value;
};

type Move =
    | { readonly op: 'reserve'; readonly quantity: number }
    | { readonly op: 'release'; readonly quantity: number }
    | { readonly op: 'restock'; readonly quantity: number };

const move = (item: StockItem, m: Move): StockItem => {
    const result = item[m.op](m.quantity);
    // A rejected move leaves the position untouched — that is the point of the
    // immutable Result API — so we simply keep the current item and move on.
    return result.isOk() ? result.value : item;
};

describe('StockItem properties', () => {
    it('property: create rejects every negative available quantity', () => {
        fc.assert(
            fc.property(fc.integer({min: -1_000_000, max: -1}), availableQuantity =>
                StockItem.create({productId: 'product-1', availableQuantity}).isErr(),
            ),
        );
    });

    it('property: create rejects every non-integer available quantity', () => {
        fc.assert(
            fc.property(
                fc.float({min: Math.fround(0.01), max: 1000, noNaN: true}).filter(n => !Number.isInteger(n)),
                availableQuantity => StockItem.create({productId: 'product-1', availableQuantity}).isErr(),
            ),
        );
    });

    it('property: reserve + release conserve total stock across any sequence of moves', () => {
        fc.assert(
            fc.property(
                fc.integer({min: 0, max: 10_000}),
                fc.array(
                    fc.oneof(
                        fc.record({op: fc.constant('reserve' as const), quantity: fc.integer({min: 1, max: 500})}),
                        fc.record({op: fc.constant('release' as const), quantity: fc.integer({min: 1, max: 500})}),
                    ),
                    {maxLength: 50},
                ),
                (initialStock, moves) => {
                    const finalItem = moves.reduce(move, okStockItem(initialStock));

                    return finalItem.availableQuantity + finalItem.reservedQuantity === initialStock;
                },
            ),
        );
    });

    it('property: every reachable state keeps both quantities non-negative', () => {
        fc.assert(
            fc.property(
                fc.integer({min: 0, max: 10_000}),
                fc.array(
                    fc.oneof(
                        fc.record({op: fc.constant('reserve' as const), quantity: fc.integer({min: 1, max: 500})}),
                        fc.record({op: fc.constant('release' as const), quantity: fc.integer({min: 1, max: 500})}),
                        fc.record({op: fc.constant('restock' as const), quantity: fc.integer({min: 1, max: 500})}),
                    ),
                    {maxLength: 50},
                ),
                (initialStock, moves) => {
                    const finalItem = moves.reduce(move, okStockItem(initialStock));

                    return finalItem.availableQuantity >= 0 && finalItem.reservedQuantity >= 0;
                },
            ),
        );
    });

    it('property: reserve never hands out more than is available', () => {
        fc.assert(
            fc.property(
                fc.integer({min: 0, max: 10_000}),
                fc.integer({min: 1, max: 20_000}),
                (available, requested) => {
                    const result = okStockItem(available).reserve(requested);

                    return requested <= available ? result.isOk() : result.isErr();
                },
            ),
        );
    });

    it('property: a successful reserve then release of the same quantity round-trips to the start', () => {
        fc.assert(
            fc.property(
                fc.integer({min: 1, max: 10_000}),
                fc.integer({min: 1, max: 10_000}),
                (available, quantity) => {
                    fc.pre(quantity <= available);

                    const start = okStockItem(available);
                    const roundTripped = start.reserve(quantity).andThen(reserved => reserved.release(quantity));

                    return (
                        roundTripped.isOk() &&
                        roundTripped.value.availableQuantity === start.availableQuantity &&
                        roundTripped.value.reservedQuantity === start.reservedQuantity
                    );
                },
            ),
        );
    });
});
