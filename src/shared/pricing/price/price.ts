import {err, ok, type Result} from 'neverthrow';
import type {PriceError} from './price-error';

const CURRENCY = 'EUR';
const MAX_AMOUNT = 100000;

export class Price {
    readonly currency = CURRENCY;

    private constructor() {}

    static create(amount: number): Result<Price, PriceError> {
        if (amount <= 0) {
            return err({kind: 'PriceAmountNotPositive', amount});
        }

        if (Math.round(amount * 100) / 100 !== amount) {
            return err({kind: 'PriceAmountTooManyDecimals', amount});
        }

        if (amount > MAX_AMOUNT) {
            return err({kind: 'PriceAmountTooHigh', amount});
        }

        return ok(new Price());
    }
}
