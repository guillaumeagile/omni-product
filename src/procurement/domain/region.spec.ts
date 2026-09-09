import {describe, expect, it} from 'vitest';
import {Region} from './region';
import {REGION_ERROR_KIND} from './region-error';

describe('Region.create', () => {
    it.each(['EU', 'UK', 'US', 'APAC'])('accepts the supported region %s', code => {
        expect(Region.create(code)._unsafeUnwrap().code).toBe(code);
    });

    it('upper-cases and trims before matching', () => {
        expect(Region.create('  eu ')._unsafeUnwrap().code).toBe('EU');
    });

    it.each(['FR', 'LATAM', 'emea', 'World', ''])('rejects the unsupported region %s', code => {
        expect(Region.create(code)._unsafeUnwrapErr()).toEqual({
            kind: REGION_ERROR_KIND.Unsupported,
            value: code.trim().toUpperCase(),
        });
    });
});

describe('Region.supported', () => {
    it('lists exactly the four supported regions', () => {
        expect([...Region.supported()]).toEqual(['EU', 'UK', 'US', 'APAC']);
    });
});

describe('Region#equals', () => {
    it('is true for the same code', () => {
        expect(Region.create('EU')._unsafeUnwrap().equals(Region.create('eu')._unsafeUnwrap())).toBe(true);
    });

    it('is false for a different code', () => {
        expect(Region.create('EU')._unsafeUnwrap().equals(Region.create('US')._unsafeUnwrap())).toBe(false);
    });
});
