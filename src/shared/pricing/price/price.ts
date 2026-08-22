import {err, ok, type Result} from 'neverthrow';
import {PRICE_ERROR_KIND, type PriceError} from './price-error';

const CURRENCY = 'EUR';
const MAX_AMOUNT = 100000;
const ROUNDING_FACTOR = 100;

export class Price {
    readonly currency = CURRENCY;

    private constructor() {}

    static create(amount: number): Result<Price, PriceError> {
        if (amount <= 0) {
            return err({kind: PRICE_ERROR_KIND.AmountNotPositive, amount});
        }

        if (Math.round(amount * ROUNDING_FACTOR) / ROUNDING_FACTOR !== amount) {
            return err({kind: PRICE_ERROR_KIND.AmountTooManyDecimals, amount});
        }

        if (amount > MAX_AMOUNT) {
            return err({kind: PRICE_ERROR_KIND.AmountTooHigh, amount});
        }

        return ok(new Price());
    }
}
