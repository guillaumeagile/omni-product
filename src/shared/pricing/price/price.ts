import {err, ok, type Result} from 'neverthrow';
import {PRICE_ERROR_KIND, type PriceError} from './price-error';
import {PRICE_WITH_TAX_ERROR_KIND, type PriceWithTaxError} from './price-with-tax-error';
import {PriceWithTax} from './price-with-tax';

const CURRENCY = 'EUR';
const MAX_AMOUNT = 100000;
const ROUNDING_FACTOR = 100;
const MIN_TAX_RATE = 0;
const MAX_TAX_RATE = 1;

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

    withTax(rate: number): Result<PriceWithTax, PriceWithTaxError> {
        if (rate < MIN_TAX_RATE || rate > MAX_TAX_RATE) {
            return err({kind: PRICE_WITH_TAX_ERROR_KIND.RateOutOfRange, rate});
        }

        return ok(new PriceWithTax(this.amount * (1 + rate)));
    }
}
