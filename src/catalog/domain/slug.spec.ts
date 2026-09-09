import {describe, expect, it} from 'vitest';
import {Slug} from './slug';
import {SLUG_ERROR_KIND} from './slug-error';

describe('Slug.create', () => {
    it.each(['red-bike', 'abc', 'a1-b2-c3', '2024-model'])('accepts a well-formed slug: %s', raw => {
        expect(Slug.create(raw)._unsafeUnwrap().value).toBe(raw);
    });

    it('trims surrounding whitespace before validating', () => {
        expect(Slug.create('  red-bike  ')._unsafeUnwrap().value).toBe('red-bike');
    });

    it.each(['ab', 'a', ''])('rejects a slug shorter than 3 characters: %s', raw => {
        const result = Slug.create(raw);

        expect(result._unsafeUnwrapErr().kind).toBe(SLUG_ERROR_KIND.TooShort);
    });

    it('reports the trimmed length on a too-short slug', () => {
        expect(Slug.create(' ab ')._unsafeUnwrapErr()).toEqual({
            kind: SLUG_ERROR_KIND.TooShort,
            value: 'ab',
            length: 2,
        });
    });

    it.each([
        'Red-Bike',
        'red_bike',
        'red bike',
        '-red-bike',
        'red-bike-',
        'red--bike',
        'red.bike',
        'café',
    ])('rejects a slug that is not lowercase-alphanumeric-with-single-hyphens: %s', raw => {
        expect(Slug.create(raw)._unsafeUnwrapErr().kind).toBe(SLUG_ERROR_KIND.InvalidFormat);
    });

    it('reports the offending value on an invalid-format slug', () => {
        expect(Slug.create('Red-Bike')._unsafeUnwrapErr()).toEqual({
            kind: SLUG_ERROR_KIND.InvalidFormat,
            value: 'Red-Bike',
        });
    });
});

describe('Slug#equals', () => {
    it('is true for two slugs with the same text', () => {
        const a = Slug.create('red-bike')._unsafeUnwrap();
        const b = Slug.create('red-bike')._unsafeUnwrap();

        expect(a.equals(b)).toBe(true);
    });

    it('is false for two slugs with different text', () => {
        const a = Slug.create('red-bike')._unsafeUnwrap();
        const b = Slug.create('blue-bike')._unsafeUnwrap();

        expect(a.equals(b)).toBe(false);
    });
});
