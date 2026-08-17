import {describe, expect, it, vi} from 'vitest';
import {
    andTee,
    andThen,
    asyncAndThen,
    asyncFilterAndThen,
    asyncMap,
    filterAndThen,
    fromNullable,
    isNone,
    isSome,
    map,
    match,
    none,
    type Option,
    OPTION_KIND,
    orElse,
    orTee,
    some,
    unwrapOr,
    unwrapOrElse,
} from './option';

describe('Option', () => {
    it('creates Some with a value', () => {
        const option = some('pricing');

        expect(option).toEqual({kind: OPTION_KIND.Some, value: 'pricing'});
    });

    it('creates None without a value', () => {
        const option = none();

        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('identifies Some values', () => {
        const option = some(42);

        expect(isSome(option)).toBe(true);
        expect(isNone(option)).toBe(false);
    });

    it('identifies None values', () => {
        const option = none<number>();

        expect(isSome(option)).toBe(false);
        expect(isNone(option)).toBe(true);
    });

    it('maps a Some value', () => {
        const option = map(some(21), value => value * 2);

        expect(option).toEqual({kind: OPTION_KIND.Some, value: 42});
    });

    it('does not map a None value', () => {
        const option = map(none<number>(), value => value * 2);

        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('async maps a Some value', async () => {
        const option = await asyncMap(some(21), async value => value * 2);

        expect(option).toEqual({kind: OPTION_KIND.Some, value: 42});
    });

    it('does not async map a None value', async () => {
        const option = await asyncMap(none<number>(), async value => value * 2);

        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('andThen chains a Some value into another Option', () => {
        const option = andThen(some(10), value => (value > 0 ? some(value.toString()) : none()));

        expect(option).toEqual({kind: OPTION_KIND.Some, value: '10'});
    });

    it('keeps None when andThen runs on None', () => {
        const option = andThen(none<number>(), value => some(value.toString()));

        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('asyncAndThen chains a Some value into another Option', async () => {
        const option = await asyncAndThen(some(10), async value => (value > 0 ? some(value.toString()) : none()));

        expect(option).toEqual({kind: OPTION_KIND.Some, value: '10'});
    });

    it('keeps None when asyncAndThen runs on None', async () => {
        const option = await asyncAndThen(none<number>(), async value => some(value.toString()));

        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('matches Some branches', () => {
        const result = match(some('margin'), value => `Some:${value}`, () => 'None');

        expect(result).toBe('Some:margin');
    });

    it('matches None branches', () => {
        const result = match(none<string>(), value => `Some:${value}`, () => 'None');

        expect(result).toBe('None');
    });

    it('creates Some from non-nullable values', () => {
        const option = fromNullable('catalog');

        expect(option).toEqual({kind: OPTION_KIND.Some, value: 'catalog'});
    });

    it('creates None from null', () => {
        const option = fromNullable<string | null>(null);

        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('creates None from undefined', () => {
        const option = fromNullable<string | undefined>(undefined);

        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('returns the inner value when present', () => {
        const result = unwrapOr(some('stock'), 'fallback');

        expect(result).toBe('stock');
    });

    it('returns the fallback when absent', () => {
        const result = unwrapOr(none<string>(), 'fallback');

        expect(result).toBe('fallback');
    });

    it('uses orElse to recover from None', () => {
        const option = orElse(none<string>(), () => some('fallback'));

        expect(option).toEqual({kind: OPTION_KIND.Some, value: 'fallback'});
    });

    it('keeps Some when orElse is called', () => {
        const option = orElse(some('catalog'), () => some('fallback'));

        expect(option).toEqual({kind: OPTION_KIND.Some, value: 'catalog'});
    });

    it('runs andTee side effects for Some and keeps the original option', () => {
        const sideEffect = vi.fn<(value: string) => void>();
        const option = andTee(some('catalog'), sideEffect);

        expect(sideEffect).toHaveBeenCalledWith('catalog');
        expect(option).toEqual({kind: OPTION_KIND.Some, value: 'catalog'});
    });

    it('does not run andTee side effects for None', () => {
        const sideEffect = vi.fn<(value: string) => void>();
        const option = andTee(none<string>(), sideEffect);

        expect(sideEffect).not.toHaveBeenCalled();
        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('runs orTee side effects for None and keeps the original option', () => {
        const sideEffect = vi.fn<() => void>();
        const option = orTee(none<string>(), sideEffect);

        expect(sideEffect).toHaveBeenCalled();
        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('does not run orTee side effects for Some', () => {
        const sideEffect = vi.fn<() => void>();
        const option = orTee(some('catalog'), sideEffect);

        expect(sideEffect).not.toHaveBeenCalled();
        expect(option).toEqual({kind: OPTION_KIND.Some, value: 'catalog'});
    });

    it('keeps the original Some when filterAndThen passes', () => {
        const option = filterAndThen(some(18), value => (value > 0 ? some('validated') : none()));

        expect(option).toEqual({kind: OPTION_KIND.Some, value: 18});
    });

    it('turns Some into None when filterAndThen fails', () => {
        const option = filterAndThen(some(0), value => (value > 0 ? some('validated') : none()));

        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('keeps None when filterAndThen runs on None', () => {
        const option = filterAndThen(none<number>(), value => some(value));

        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('keeps the original Some when asyncFilterAndThen passes', async () => {
        const option = await asyncFilterAndThen(some(18), async value => (value > 0 ? some('validated') : none()));

        expect(option).toEqual({kind: OPTION_KIND.Some, value: 18});
    });

    it('turns Some into None when asyncFilterAndThen fails', async () => {
        const option = await asyncFilterAndThen(some(0), async value => (value > 0 ? some('validated') : none()));

        expect(option).toEqual({kind: OPTION_KIND.None});
    });

    it('returns the inner value when unwrapOrElse runs on Some', () => {
        const fallback = vi.fn(() => 'fallback');

        expect(unwrapOrElse(some('stock'), fallback)).toBe('stock');
        expect(fallback).not.toHaveBeenCalled();
    });

    it('computes the fallback lazily when unwrapOrElse runs on None', () => {
        const fallback = vi.fn(() => 'fallback');

        expect(unwrapOrElse(none<string>(), fallback)).toBe('fallback');
        expect(fallback).toHaveBeenCalledOnce();
    });
});

describe('Option composition', () => {
    it('chains map and match into a domain-readable message', () => {
        const maybeMargin: Option<number> = some(18);
        const message = match(map(maybeMargin, margin => `${margin}%`), margin => `Margin is ${margin}`, () => 'Margin is missing');

        expect(message).toBe('Margin is 18%');
    });
});
