import {describe, expect, it} from 'vitest';
import {ProductTitle} from './product-title';
import {PRODUCT_TITLE_ERROR_KIND} from './product-title-error';

describe('ProductTitle.create', () => {
    it('accepts a plain title', () => {
        expect(ProductTitle.create('Red Bike')._unsafeUnwrap().value).toBe('Red Bike');
    });

    it('trims and collapses internal whitespace', () => {
        expect(ProductTitle.create('  Red   Bike  ')._unsafeUnwrap().value).toBe('Red Bike');
    });

    it('rejects an empty title', () => {
        expect(ProductTitle.create('')._unsafeUnwrapErr()).toEqual({
            kind: PRODUCT_TITLE_ERROR_KIND.Empty,
        });
    });

    it('rejects a whitespace-only title as empty', () => {
        expect(ProductTitle.create('   ')._unsafeUnwrapErr().kind).toBe(PRODUCT_TITLE_ERROR_KIND.Empty);
    });

    it('accepts a title of exactly 200 characters', () => {
        expect(ProductTitle.create('a'.repeat(200))._unsafeUnwrap().value.length).toBe(200);
    });

    it('rejects a title longer than 200 characters and reports its length', () => {
        expect(ProductTitle.create('a'.repeat(201))._unsafeUnwrapErr()).toEqual({
            kind: PRODUCT_TITLE_ERROR_KIND.TooLong,
            length: 201,
        });
    });
});

describe('ProductTitle#equals', () => {
    it('is true when the normalized text matches', () => {
        const a = ProductTitle.create('Red Bike')._unsafeUnwrap();
        const b = ProductTitle.create('  Red   Bike ')._unsafeUnwrap();

        expect(a.equals(b)).toBe(true);
    });

    it('is false when the text differs', () => {
        const a = ProductTitle.create('Red Bike')._unsafeUnwrap();
        const b = ProductTitle.create('Blue Bike')._unsafeUnwrap();

        expect(a.equals(b)).toBe(false);
    });
});
