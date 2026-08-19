import {describe, expect, it} from 'vitest';
import * as fc from 'fast-check';
import {Margin} from './margin';

const okMargin = (percentage: number): Margin => {
    const result = Margin.fromPercentage(percentage);
    if (result.isErr()) {
        throw new Error(`expected a valid margin for ${percentage}%`);
    }
    return result.value;
};

describe('Margin.fromPercentage', () => {
    it('builds a margin from a whole percentage', () => {
        const result = Margin.fromPercentage(18);

        expect(result.isOk()).toBe(true);
        expect(result._unsafeUnwrap().applyTo(100)).toBe(18);
    });

    it('accepts the lower bound of 5%', () => {
        const result = Margin.fromPercentage(5);

        expect(result.isOk()).toBe(true);
        expect(result._unsafeUnwrap().applyTo(100)).toBe(5);
    });

    it('accepts the upper bound of 100%', () => {
        const result = Margin.fromPercentage(100);

        expect(result.isOk()).toBe(true);
        expect(result._unsafeUnwrap().applyTo(100)).toBe(100);
    });

    it('rejects a negative percentage', () => {
        const result = Margin.fromPercentage(-5);

        expect(result.isErr()).toBe(true);
        expect(result._unsafeUnwrapErr()).toEqual({kind: 'MarginPercentageOutOfRange', percentage: -5});
    });

    it('rejects a percentage above 100', () => {
        const result = Margin.fromPercentage(150);

        expect(result.isErr()).toBe(true);
        expect(result._unsafeUnwrapErr()).toEqual({kind: 'MarginPercentageOutOfRange', percentage: 150});
    });

    it('rejects a percentage under 5%', () => {
        const result = Margin.fromPercentage(4);

        expect(result.isErr()).toBe(true);
        expect(result._unsafeUnwrapErr()).toEqual({kind: 'MarginPercentageOutOfRange', percentage: 4    });
    });

    it('rejects NaN as not finite', () => {
        const result = Margin.fromPercentage(Number.NaN);

        expect(result.isErr()).toBe(true);
        expect(result._unsafeUnwrapErr().kind).toBe('MarginPercentageNotFinite');
    });

    it('rejects Infinity as not finite', () => {
        const result = Margin.fromPercentage(Number.POSITIVE_INFINITY);

        expect(result.isErr()).toBe(true);
        expect(result._unsafeUnwrapErr().kind).toBe('MarginPercentageNotFinite');
    });
});

describe('Margin.forRegion', () => {
    it.each([
        ['EU', 18],
        ['UK', 22],
        ['US', 15],
        ['APAC', 25],
    ])('uses the regional rate for %s', (region, expectedMarkup) => {
        const margin = Margin.forRegion(region);

        expect(margin.applyTo(100)).toBe(expectedMarkup);
    });

    it('falls back to the default rate for an unknown region', () => {
        const margin = Margin.forRegion('MARS');

        expect(margin.applyTo(100)).toBe(20);
    });

});

describe('Margin.default', () => {
    it('uses the default rate', () => {
        const margin = Margin.default();

        expect(margin.applyTo(100)).toBe(20);
    });
});

describe('Margin#applyTo', () => {
    it('computes the markup amount for a base price', () => {
        const margin = okMargin(20);

        expect(margin.applyTo(100)).toBe(20);
    });

    it('rounds the markup to 2 decimal places', () => {
        const margin = okMargin(33);

        expect(margin.applyTo(10)).toBe(3.3);
    });

    it('produces the minimum markup for the lower-bound margin', () => {
        const margin = okMargin(5);

        expect(margin.applyTo(100)).toBe(5);
    });
});

describe('Margin#equals', () => {
    it('treats two margins with the same rate as equal', () => {
        expect(okMargin(18).equals(okMargin(18))).toBe(true);
    });

    it('treats two margins with different rates as not equal', () => {
        expect(okMargin(18).equals(okMargin(22))).toBe(false);
    });
});

describe('Margin properties', () => {
    it('property: applyTo never produces a markup outside [0, baseAmount] for non-negative amounts', () => {
        // Rounding to 2 decimals can nudge the markup a hair above the raw amount
        // (e.g. 2.945 at 100% rounds to 2.95); allow that rounding tolerance.
        const ROUNDING_TOLERANCE = 0.005;

        fc.assert(
            fc.property(
                fc.float({min: 0, max: 1_000_000, noNaN: true}),
                fc.integer({min: 5, max: 100}),
                (amount, percentage) => {
                    const markup = okMargin(percentage).applyTo(amount);

                    return markup >= 0 && markup <= amount + ROUNDING_TOLERANCE;
                },
            ),
        );
    });

    it('property: the lower-bound margin never yields a markup above the base amount', () => {
        fc.assert(
            fc.property(fc.float({min: 0, max: 1_000_000, noNaN: true}), amount => okMargin(5).applyTo(amount) <= amount),
        );
    });

    it('property: a 100% margin always yields the base amount back, rounded', () => {
        fc.assert(
            fc.property(
                fc.float({min: 0, max: 1_000_000, noNaN: true}),
                amount => okMargin(100).applyTo(amount) === Math.round(amount * 100) / 100,
            ),
        );
    });

    it('property: applyTo is monotonic in the margin rate for a fixed positive amount', () => {
        fc.assert(
            fc.property(
                fc.float({min: 1, max: 1_000_000, noNaN: true}),
                fc.integer({min: 5, max: 99}),
                (amount, lowerPercentage) => {
                    const lower = okMargin(lowerPercentage).applyTo(amount);
                    const higher = okMargin(lowerPercentage + 1).applyTo(amount);

                    return higher >= lower;
                },
            ),
        );
    });

    it('property: fromPercentage rejects every percentage outside [5, 100]', () => {
        fc.assert(
            fc.property(
                fc.float({noNaN: true}).filter(percentage => percentage < 5 || percentage > 100),
                percentage => Margin.fromPercentage(percentage).isErr(),
            ),
        );
    });

    it('property: equals is reflexive for any valid margin', () => {
        fc.assert(
            fc.property(fc.integer({min: 5, max: 100}), percentage => {
                const margin = okMargin(percentage);

                return margin.equals(margin);
            }),
        );
    });
});
