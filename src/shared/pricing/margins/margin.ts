import {err, ok, type Result} from 'neverthrow';
import type {MarginError} from './margin-error';

type Rate = number & { readonly __brand: 'Margin.Rate' };

const MIN_PERCENTAGE = 5;
const MAX_PERCENTAGE = 100;
const PERCENTAGE_TO_RATE = 100;
const ROUNDING_FACTOR = 100;

const REGIONAL_MARGIN_PERCENTAGE: Readonly<Record<string, number>> = {
    EU: 18,
    UK: 22,
    US: 15,
    APAC: 25,
};

const DEFAULT_MARGIN_PERCENTAGE = 20;

const toRate = (percentage: number): Rate => (percentage / PERCENTAGE_TO_RATE) as Rate;

/**
 * A margin rate, always valid: the private constructor means no `Margin`
 * can exist anywhere in the codebase without having passed `fromPercentage`.
 */
export class Margin {
    private constructor(private readonly rate: Rate) {}

    /**
     * Builds a Margin from a human-entered percentage (e.g. `18` for 18%).
     * Rejects percentages outside `[5, 100]` and non-finite input.
     */
    static fromPercentage(percentage: number): Result<Margin, MarginError> {
        if (!Number.isFinite(percentage)) {
            return err({kind: 'MarginPercentageNotFinite', percentage});
        }

        if (percentage < MIN_PERCENTAGE || percentage > MAX_PERCENTAGE) {
            return err({kind: 'MarginPercentageOutOfRange', percentage});
        }

        return ok(new Margin(toRate(percentage)));
    }

    /**
     * Builds a Margin for a region, falling back to the default rate for an
     * unrecognized region. Total: regional rates are trusted constants, so
     * no invalid Margin can result and no Result is needed.
     */
    static forRegion(region: string): Margin {
        const percentage = REGIONAL_MARGIN_PERCENTAGE[region] ?? DEFAULT_MARGIN_PERCENTAGE;

        return new Margin(toRate(percentage));
    }

    /** Builds a Margin at the default rate, for when no region applies. */
    static default(): Margin {
        return new Margin(toRate(DEFAULT_MARGIN_PERCENTAGE));
    }

    /**
     * Applies the margin to a base amount, rounded to 2 decimal places.
     * Closure of operations: Margin + amount -> amount, staying in domain terms.
     */
    applyTo(baseAmount: number): number {
        return Math.round(baseAmount * this.rate * ROUNDING_FACTOR) / ROUNDING_FACTOR;
    }

    /** Value equality: two margins with the same rate are the same Margin. */
    equals(other: Margin): boolean {
        return this.rate === other.rate;
    }
}
