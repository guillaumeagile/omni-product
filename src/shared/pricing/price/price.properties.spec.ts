import {describe, it} from 'vitest';
import * as fc from 'fast-check';
import {Price} from './price';

const okPrice = (amount: number): Price => {
    const result = Price.create(amount);
    if (result.isErr()) {
        throw new Error(`expected a valid price for ${amount}`);
    }
    return result.value;
};

describe('Price properties', () => {
    it('property: create always returns EUR as the currency for any valid amount', () => {
        fc.assert(
            fc.property(fc.float({
                min: Math.fround(0.01),
                max: 100_000,
                noNaN: true
            }), amount => okPrice(amount).currency === 'EUR'),
        );
    });

    it('property: create never produces a non-positive amount', () => {
        fc.assert(
            fc.property(fc.float({
                min: Math.fround(0.01),
                max: 100_000,
                noNaN: true
            }), amount => okPrice(amount).amount > 0),
        );
    });

    it('property: create never produces an amount above 100000', () => {
        fc.assert(
            fc.property(fc.float({
                min: Math.fround(0.01),
                max: 100_000,
                noNaN: true
            }), amount => okPrice(amount).amount <= 100_000),
        );
    });

    it('property: create rejects every amount that is not positive', () => {
        fc.assert(fc.property(fc.float({max: 0, noNaN: true}), amount => Price.create(amount).isErr()));
    });

    it('property: create rejects every amount above 100000', () => {
        fc.assert(
            fc.property(
                fc.float({min: Math.fround(100_000.01), max: 1_000_000, noNaN: true}),
                amount => Price.create(amount).isErr(),
            ),
        );
    });

    it('property: create never produces an amount with more than 2 decimal places', () => {
        fc.assert(
            fc.property(fc.float({min: Math.fround(0.01), max: 100_000, noNaN: true}), amount => {
                const rounded = okPrice(amount).amount;

                return Number.isInteger(Math.round(rounded * 100));
            }),
        );
    });

    it('property: withTax rejects every rate outside [0, 1]', () => {
        fc.assert(
            fc.property(
                fc.float({noNaN: true}).filter(rate => rate < 0 || rate > 1),
                rate => okPrice(100).withTax(rate).isErr(),
            ),
        );
    });

    it('property: withTax never produces an amount below the original price for a non-negative rate', () => {
        fc.assert(
            fc.property(
                fc.float({min: Math.fround(0.01), max: 100_000, noNaN: true}),
                fc.float({min: 0, max: 1, noNaN: true}),
                (amount, rate) => {
                    const price = okPrice(amount);
                    const result = price.withTax(rate);

                    return result.isOk() && result.value.amount >= price.amount;
                },
            ),
        );
    });

    it('property: withTax is monotonic in the rate for a fixed price', () => {
        fc.assert(
            fc.property(
                fc.float({min: Math.fround(0.01), max: 100_000, noNaN: true}),
                fc.float({min: 0, max: Math.fround(0.99), noNaN: true}),
                (amount, lowerRate) => {
                    const price = okPrice(amount);
                    const lower = price.withTax(lowerRate);
                    const higher = price.withTax(lowerRate + 0.01);

                    return lower.isOk() && higher.isOk() && higher.value.amount >= lower.value.amount;
                },
            ),
        );
    });

    it('property: a 0% tax rate always yields the original amount back', () => {
        fc.assert(
            fc.property(fc.float({min: Math.fround(0.01), max: 100_000, noNaN: true}), amount => {
                const price = okPrice(amount);
                const result = price.withTax(0);

                return result.isOk() && result.value.amount === price.amount;
            }),
        );
    });
});
