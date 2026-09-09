import {describe, expect, it} from 'vitest';
import {SupplierId} from './supplier-id';
import {SUPPLIER_ID_ERROR_KIND} from './supplier-id-error';

describe('SupplierId.create', () => {
    it('accepts a non-empty id', () => {
        expect(SupplierId.create('supplier-1')._unsafeUnwrap().value).toBe('supplier-1');
    });

    it('trims surrounding whitespace', () => {
        expect(SupplierId.create('  supplier-1  ')._unsafeUnwrap().value).toBe('supplier-1');
    });

    it('rejects an empty id', () => {
        expect(SupplierId.create('')._unsafeUnwrapErr()).toEqual({kind: SUPPLIER_ID_ERROR_KIND.Empty});
    });

    it('rejects a whitespace-only id', () => {
        expect(SupplierId.create('   ')._unsafeUnwrapErr().kind).toBe(SUPPLIER_ID_ERROR_KIND.Empty);
    });
});

describe('SupplierId#equals', () => {
    it('is true for the same value', () => {
        const a = SupplierId.create('supplier-1')._unsafeUnwrap();
        const b = SupplierId.create('supplier-1')._unsafeUnwrap();

        expect(a.equals(b)).toBe(true);
    });

    it('is false for a different value', () => {
        const a = SupplierId.create('supplier-1')._unsafeUnwrap();
        const b = SupplierId.create('supplier-2')._unsafeUnwrap();

        expect(a.equals(b)).toBe(false);
    });
});
