import {describe, expect, it} from 'vitest';
import {Price} from './price';

describe('Price.create', () => {
    it('returns Ok for a positive amount', () => {
        const result = Price.create(19.99);

        expect(result.isOk()).toBe(true);
    });

    it('returns Err for a negative amount', () => {
        const result = Price.create(-1);

        expect(result.isErr()).toBe(true);
        expect(result._unsafeUnwrapErr()).toEqual({kind: 'PriceAmountNotPositive', amount: -1});
    });

    it('returns Err for a zero amount', () => {
        const result = Price.create(0);

        expect(result.isErr()).toBe(true);
        expect(result._unsafeUnwrapErr()).toEqual({kind: 'PriceAmountNotPositive', amount: 0});
    });

    it('returns Err for an amount with more than 2 decimal places', () => {
        const result = Price.create(19.999);

        expect(result.isErr()).toBe(true);
        expect(result._unsafeUnwrapErr()).toEqual({kind: 'PriceAmountTooManyDecimals', amount: 19.999});
    });

    it('returns Ok for an integer amount with no decimal places', () => {
        const result = Price.create(20);

        expect(result.isOk()).toBe(true);
    });

    it('exposes EUR as its currency', () => {
        const result = Price.create(20);

        expect(result._unsafeUnwrap().currency).toBe('EUR');
    });

    it('returns Err for an amount over 100000', () => {
        const result = Price.create(100001);

        expect(result.isErr()).toBe(true);
        expect(result._unsafeUnwrapErr()).toEqual({kind: 'PriceAmountTooHigh', amount: 100001});
    });

    it('returns Ok for an amount of exactly 100000', () => {
        const result = Price.create(100000);

        expect(result.isOk()).toBe(true);
    });
});
