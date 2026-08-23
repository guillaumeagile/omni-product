import {PriceWithTax} from './price-with-tax';

const MAX_AMOUNT = 100000;
const DECIMAL_PLACES = 2;

export class Price {
    readonly currency = 'EUR';

    private constructor(readonly amount: number) {}

    static create(amount: number): Price {
        if (amount <= 0) {
            throw new Error(`Price amount must be positive, got ${amount}`);
        }
        if (amount > MAX_AMOUNT) {
            throw new Error(`Price amount must not exceed ${MAX_AMOUNT}, got ${amount}`);
        }

        const roundedAmount = Number(amount.toFixed(DECIMAL_PLACES));

        return new Price(roundedAmount);
    }

    withTax(rate: number): PriceWithTax {
        if (rate < 0 || rate > 1) {
            throw new Error(`Tax rate must be between 0 and 1, got ${rate}`);
        }

        const amountWithTax = Number((this.amount * (1 + rate)).toFixed(DECIMAL_PLACES));

        return new PriceWithTax(amountWithTax, this.currency);
    }
}
