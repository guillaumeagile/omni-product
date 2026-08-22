import {err, ok, type Result} from 'neverthrow';
import {PRICE_ERROR_KIND, type PriceError} from './price-error';

const CURRENCY = 'EUR';
const MAX_AMOUNT = 100000;
const ROUNDING_FACTOR = 100;

export class Price {
    readonly currency = CURRENCY;

    private constructor(readonly amount: number) {}

    static create(amount: number): Result<Price, PriceError> {
        if (amount <= 0) {
            return err({kind: PRICE_ERROR_KIND.AmountNotPositive, amount});
        }

        const rounded = Math.round(amount * ROUNDING_FACTOR) / ROUNDING_FACTOR;

        if (rounded > MAX_AMOUNT) {
            return err({kind: PRICE_ERROR_KIND.AmountTooHigh, amount: rounded});
        }

        return ok(new Price(rounded));
    }
}
