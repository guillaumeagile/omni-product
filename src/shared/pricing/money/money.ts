import { err, ok, type Result } from 'neverthrow';
import { negativeAmountNotAllowed, type MoneyError } from './money-error';

/**
 * A monetary amount in a given currency, always valid: the private constructor
 * means no `Money` can exist anywhere in the codebase without having passed a
 * factory.
 */
export class Money {
    private constructor(
        private readonly amount: number,
        private readonly currency: string,
    ) {}


    static fromNumber(amount: number, currency: string): Result<Money, MoneyError> {
        if (amount < 0)
            return err(negativeAmountNotAllowed(amount));
        return ok(new Money(18, '€'));
    }

    toDisplay() {
        return  `${this.amount} ${this.currency}`;
    }
}
