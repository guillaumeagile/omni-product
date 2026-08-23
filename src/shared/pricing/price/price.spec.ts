import {describe, expect, it} from 'vitest';
import {Price} from './price';

describe('Price.create', () => {
    it('creates a price for a positive amount', () => {
        const price = Price.create(19.99);

        expect(price.amount).toBe(19.99);
    });

    it('throws for a negative amount', () => {
        expect(() => Price.create(-1)).toThrow('Price amount must be positive, got -1');
    });

    it('throws for a zero amount', () => {
        expect(() => Price.create(0)).toThrow('Price amount must be positive, got 0');
    });

    it('rounds an amount with more than 2 decimal places instead of rejecting it', () => {
        const price = Price.create(19.999);

        expect(price.amount).toBe(20);
    });

    it('creates a price for an integer amount with no decimal places', () => {
        const price = Price.create(20);

        expect(price.amount).toBe(20);
    });

    it('exposes EUR as its currency', () => {
        const price = Price.create(20);

        expect(price.currency).toBe('EUR');
    });

    it('throws for an amount over 100000', () => {
        expect(() => Price.create(100001)).toThrow('Price amount must not exceed 100000, got 100001');
    });

    it('creates a price for an amount of exactly 100000', () => {
        const price = Price.create(100000);

        expect(price.amount).toBe(100000);
    });
});

describe('Price#withTax', () => {
    it('produces a tax-inclusive price from a tax rate', () => {
        const price = Price.create(100);

        const priceWithTax = price.withTax(0.2);

        expect(priceWithTax.amount).toBe(120);
    });

    it('throws for a tax rate above 100%', () => {
        const price = Price.create(100);

        expect(() => price.withTax(1.5)).toThrow('Tax rate must be between 0 and 1, got 1.5');
    });

    it('accepts a tax rate of exactly 0%', () => {
        const price = Price.create(100);

        const priceWithTax = price.withTax(0);

        expect(priceWithTax.amount).toBe(100);
    });

    it('throws for a negative tax rate', () => {
        const price = Price.create(100);

        expect(() => price.withTax(-0.1)).toThrow('Tax rate must be between 0 and 1, got -0.1');
    });

    it('accepts a tax rate of exactly 100%', () => {
        const price = Price.create(100);

        const priceWithTax = price.withTax(1);

        expect(priceWithTax.amount).toBe(200);
    });

    it('exposes EUR as its currency', () => {
        const price = Price.create(100);

        const priceWithTax = price.withTax(0.2);

        expect(priceWithTax.currency).toBe('EUR');
    });
});
