import {err, ok, type Result} from 'neverthrow';
import type {PriceError} from './price-error';

export class Price {
    private constructor() {}

    static create(amount: number, currency?: string): Result<Price, PriceError> {
        if (amount <= 0) {
            return err({kind: 'PriceAmountNotPositive', amount});
        }

        if (Math.round(amount * 100) / 100 !== amount) {
            return err({kind: 'PriceAmountTooManyDecimals', amount});
        }

        return ok(new Price());
    }
}
