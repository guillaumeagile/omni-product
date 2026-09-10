import { describe, expect, it } from 'vitest';
//import * as fc from 'fast-check';
import { Money } from './money';

const EUROS = '€';

describe('Money', () => {
    it('builds a VO from a number and currency', () => {

        const result = Money.fromNumber(18, EUROS);

        expect(result.isOk()).toBe(true);
        expect(result._unsafeUnwrap().toDisplay()).toBe("18 €");
    });

    it('cannot build a VO from a neg number and currency', () => {
        const result = Money.fromNumber(-0.0000000001, EUROS);

        expect(result.isErr()).toBe(true);
        expect(result._unsafeUnwrapErr()).toEqual({ amount: -1e-10, kind: 'NegativeAmountNotAllowed' });
    });


});