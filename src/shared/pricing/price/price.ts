import {err, ok, type Result} from 'neverthrow';
import type {PriceError} from './price-error';

const CURRENCY = 'EUR';

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

        return ok(new Price());
    }
}
