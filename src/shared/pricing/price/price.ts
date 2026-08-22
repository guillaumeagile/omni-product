import {ok, type Result} from 'neverthrow';
import type {PriceError} from './price-error';

export class Price {
    private constructor() {}

    static create(amount: number, currency?: string): Result<Price, PriceError> {
        return ok(new Price());
    }
}
